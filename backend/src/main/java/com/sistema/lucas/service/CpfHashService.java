package com.sistema.lucas.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * AUD-03 (LGPD): Gera hash HMAC-SHA256 do CPF usando um pepper secreto,
 * tornando o hash irreversível por força bruta mesmo para o universo
 * limitado de ~200 milhões de CPFs válidos.
 */
@Component
public class CpfHashService {

    private final SecretKeySpec hmacKey;

    public CpfHashService(@Value("${api.security.cpf-hash.pepper}") String pepper) {
        this.hmacKey = new SecretKeySpec(pepper.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    /**
     * Gera o HMAC-SHA256 de um CPF limpo (somente dígitos).
     */
    public String hash(String cleanCpf) {
        if (cleanCpf == null) return null;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(hmacKey);
            byte[] result = mac.doFinal(cleanCpf.replaceAll("[^0-9]", "").getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : result) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar HMAC do CPF", e);
        }
    }
}
