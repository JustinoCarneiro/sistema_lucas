package com.sistema.lucas.config.jpa;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Component
@Converter
public class EncryptionConverter implements AttributeConverter<String, String> {

    private static final String GCM_ALGORITHM = "AES/GCM/NoPadding";
    private static final String ECB_ALGORITHM = "AES/ECB/PKCS5Padding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;
    private static final String GCM_PREFIX = "GCM:";
    private final SecretKeySpec key;
    private final SecureRandom secureRandom = new SecureRandom();

    public EncryptionConverter(@Value("${api.security.encryption.key}") String encryptionKey) {
        // Garantir que a chave tenha 16 bytes para AES-128
        byte[] keyBytes = new byte[16];
        byte[] providedBytes = encryptionKey.getBytes();
        System.arraycopy(providedBytes, 0, keyBytes, 0, Math.min(providedBytes.length, 16));
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(GCM_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(attribute.getBytes());
            byte[] combined = new byte[IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(encrypted, 0, combined, IV_LENGTH, encrypted.length);
            return GCM_PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao criptografar dado", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            // Formato novo: AES/GCM com IV embutido
            if (dbData.startsWith(GCM_PREFIX)) {
                byte[] combined = Base64.getDecoder().decode(dbData.substring(GCM_PREFIX.length()));
                byte[] iv = new byte[IV_LENGTH];
                System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
                byte[] ciphertext = new byte[combined.length - IV_LENGTH];
                System.arraycopy(combined, IV_LENGTH, ciphertext, 0, ciphertext.length);
                Cipher cipher = Cipher.getInstance(GCM_ALGORITHM);
                cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH, iv));
                return new String(cipher.doFinal(ciphertext));
            }
            // Formato legado: AES/ECB (migração automática na próxima gravação)
            Cipher cipher = Cipher.getInstance(ECB_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            return new String(cipher.doFinal(Base64.getDecoder().decode(dbData)));
        } catch (Exception e) {
            // Dado sem criptografia (pré-migração) — retorna o original
            return dbData;
        }
    }
}
