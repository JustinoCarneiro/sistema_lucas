// backend/src/main/java/com/sistema/lucas/security/service/TotpService.java
package com.sistema.lucas.security.service;

import org.bouncycastle.util.encoders.Base32;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;

/**
 * MFA (SEC-02): implementação de TOTP (RFC 6238) escrita à mão com javax.crypto, no mesmo
 * estilo de CpfHashService/EncryptionConverter — o projeto já implementa primitivas de cripto
 * diretamente em vez de terceirizar pra uma lib de alto nível.
 *
 * O Clock é injetado (não Instant.now() direto) justamente pra evitar o antipadrão de teste já
 * documentado em memoria-tecnica/bugs/teste-time-bomb-lembrete-scheduler.md — os testes fixam
 * o Clock, não dependem do relógio real da máquina.
 */
@Service
public class TotpService {

    private static final int STEP_SECONDS = 30;
    private static final int DIGITS = 6;
    // Tolerância de ±1 passo (RFC 6238) pra absorver clock drift entre o servidor e o
    // autenticador do usuário — janela efetiva de ~90s.
    private static final int WINDOW = 1;

    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public TotpService() {
        this(Clock.systemUTC());
    }

    // Construtor usado em teste, pra fixar o relógio.
    public TotpService(Clock clock) {
        this.clock = clock;
    }

    /** Gera um secret novo, codificado em Base32 (formato padrão de apps autenticadores). */
    public String generateSecretBase32() {
        byte[] raw = new byte[20]; // 160 bits, padrão pra HmacSHA1
        secureRandom.nextBytes(raw);
        return new String(Base32.encode(raw), java.nio.charset.StandardCharsets.US_ASCII);
    }

    /** Monta a URI otpauth:// que os apps autenticadores leem via QR code. */
    public String buildOtpAuthUri(String secretBase32, String accountEmail) {
        String issuer = "InstitutoLucas";
        return "otpauth://totp/" + issuer + ":" + accountEmail
            + "?secret=" + secretBase32
            + "&issuer=" + issuer
            + "&digits=" + DIGITS
            + "&period=" + STEP_SECONDS;
    }

    /** Confere um código de 6 dígitos contra o secret, tolerando ±1 passo de clock drift. */
    public boolean verifyCode(String secretBase32, String code) {
        if (secretBase32 == null || code == null || !code.matches("\\d{" + DIGITS + "}")) {
            return false;
        }
        long currentCounter = Instant.now(clock).getEpochSecond() / STEP_SECONDS;
        for (int i = -WINDOW; i <= WINDOW; i++) {
            if (code.equals(generateCode(secretBase32, currentCounter + i))) {
                return true;
            }
        }
        return false;
    }

    private String generateCode(String secretBase32, long counter) {
        try {
            byte[] key = Base32.decode(secretBase32);
            byte[] counterBytes = ByteBuffer.allocate(8).putLong(counter).array();

            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(counterBytes);

            // Truncamento dinâmico (RFC 4226 §5.3)
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8)
                | (hash[offset + 3] & 0xFF);

            int otp = binary % (int) Math.pow(10, DIGITS);
            return String.format("%0" + DIGITS + "d", otp);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar código TOTP", e);
        }
    }
}
