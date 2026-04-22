package com.sistema.lucas.config.jpa;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Component
@Converter
public class EncryptionConverter implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/ECB/PKCS5Padding";
    private final SecretKeySpec key;

    public EncryptionConverter(@Value("${api.security.encryption.key}") String encryptionKey) {
        // Garantir que a chave tenha 16 bytes para AES-128 (simples para demo)
        byte[] keyBytes = new byte[16];
        byte[] providedBytes = encryptionKey.getBytes();
        System.arraycopy(providedBytes, 0, keyBytes, 0, Math.min(providedBytes.length, 16));
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return Base64.getEncoder().encodeToString(cipher.doFinal(attribute.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Erro ao criptografar dado", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            return new String(cipher.doFinal(Base64.getDecoder().decode(dbData)));
        } catch (Exception e) {
            // Caso o dado já estivesse no banco sem criptografia, retorna o original (facilita migração)
            return dbData;
        }
    }
}
