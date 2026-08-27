// backend/src/main/java/com/sistema/lucas/service/MfaBackupCodeService.java
package com.sistema.lucas.service;

import com.sistema.lucas.model.MfaBackupCode;
import com.sistema.lucas.model.User;
import com.sistema.lucas.repository.MfaBackupCodeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * MFA (SEC-02): códigos de backup de uso único. Mesmo padrão HMAC-SHA256+pepper de
 * CpfHashService, mas com uma pepper PRÓPRIA (api.security.mfa-backup.pepper) — nunca reusar
 * pepper entre domínios de hash diferentes.
 */
@Service
public class MfaBackupCodeService {

    private static final int QUANTIDADE = 10;
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // sem O/0/I/1 (ambíguos)
    private static final int TAMANHO_CODIGO = 10;

    private final SecretKeySpec hmacKey;
    private final MfaBackupCodeRepository repository;
    private final SecureRandom secureRandom = new SecureRandom();

    public MfaBackupCodeService(
            @Value("${api.security.mfa-backup.pepper}") String pepper,
            MfaBackupCodeRepository repository) {
        this.hmacKey = new SecretKeySpec(pepper.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.repository = repository;
    }

    private String hash(String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(hmacKey);
            byte[] result = mac.doFinal(code.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : result) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar hash do código de backup", e);
        }
    }

    private String gerarCodigoAleatorio() {
        StringBuilder sb = new StringBuilder(TAMANHO_CODIGO);
        for (int i = 0; i < TAMANHO_CODIGO; i++) {
            sb.append(ALFABETO.charAt(secureRandom.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }

    /** Gera QUANTIDADE códigos novos pro usuário, apaga os antigos, e devolve os valores em
     * texto plano — únicos momento em que o valor bruto existe fora da memória do cliente. */
    public List<String> gerarNovos(User user) {
        repository.deleteByUser(user);

        List<String> codigosEmTextoPlano = new ArrayList<>(QUANTIDADE);
        List<MfaBackupCode> entidades = new ArrayList<>(QUANTIDADE);
        for (int i = 0; i < QUANTIDADE; i++) {
            String codigo = gerarCodigoAleatorio();
            codigosEmTextoPlano.add(codigo);

            var entidade = new MfaBackupCode();
            entidade.setUser(user);
            entidade.setCodeHash(hash(codigo));
            entidades.add(entidade);
        }
        repository.saveAll(entidades);
        return codigosEmTextoPlano;
    }

    /** Apaga todos os backup codes do usuário (chamado ao desativar o MFA — não faz sentido
     * deixar códigos órfãos de um MFA que não existe mais). */
    public void apagarTodos(User user) {
        repository.deleteByUser(user);
    }

    /** Confere e consome (uso único) um código de backup. UPDATE condicional atômico por
     * dentro — fecha a corrida de duas requisições tentando consumir o mesmo código. */
    public boolean consumirSeValido(User user, String codigoDigitado) {
        if (codigoDigitado == null || codigoDigitado.isBlank()) return false;
        String hashDigitado = hash(codigoDigitado.trim().toUpperCase());

        return repository.findByUserAndUsedAtIsNull(user).stream()
            .filter(c -> c.getCodeHash().equals(hashDigitado))
            .findFirst()
            .map(c -> repository.marcarUsadoSeAindaNaoUsado(c.getId(), LocalDateTime.now()) == 1)
            .orElse(false);
    }
}
