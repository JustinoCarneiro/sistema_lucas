package com.sistema.lucas.config.jpa;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * AES-256-GCM encryption converter for sensitive JPA fields.
 *
 * <p>Multi-key design: one primary key (used for all new writes) plus an optional
 * CSV of legacy keys ({@code api.security.encryption.legacy-keys}). Each legacy key
 * produces both a 32-byte and a 16-byte GCM candidate, plus a 16-byte ECB candidate,
 * covering AES-256 weak-key rotation, AES-128 GCM, and ECB plaintext-era data.
 *
 * <p>AUD-02: {@code convertToEntityAttribute} is strict — any decryption failure is
 * an auditable error. Use {@code decryptLenient} exclusively in migration runners
 * where plaintext legacy data must be tolerated.
 */
@Component
@Converter
public class EncryptionConverter implements AttributeConverter<String, String> {

    private static final Logger log = LoggerFactory.getLogger(EncryptionConverter.class);
    private static final String GCM_ALGORITHM = "AES/GCM/NoPadding";
    private static final String ECB_ALGORITHM = "AES/ECB/PKCS5Padding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;
    private static final String GCM_PREFIX = "GCM:";

    private final SecretKeySpec primaryKey;
    private final List<SecretKeySpec> allGcmKeys;
    private final List<SecretKeySpec> ecbFallbackKeys;
    private final SecureRandom secureRandom = new SecureRandom();

    public EncryptionConverter(
            @Value("${api.security.encryption.key}") String encryptionKey,
            @Value("${api.security.encryption.legacy-keys:}") String legacyKeysCSV) {

        this.primaryKey = toKey(encryptionKey, 32);

        List<SecretKeySpec> gcmList = new ArrayList<>();
        gcmList.add(this.primaryKey);
        List<SecretKeySpec> ecbList = new ArrayList<>();

        if (legacyKeysCSV != null && !legacyKeysCSV.isBlank()) {
            for (String raw : legacyKeysCSV.split(",")) {
                String k = raw.strip();
                if (k.isEmpty()) continue;
                gcmList.add(toKey(k, 32));
                gcmList.add(toKey(k, 16));
                ecbList.add(toKey(k, 16));
            }
        }

        this.allGcmKeys = List.copyOf(gcmList);
        this.ecbFallbackKeys = List.copyOf(ecbList);
    }

    private static SecretKeySpec toKey(String raw, int size) {
        byte[] keyBytes = new byte[size];
        byte[] src = raw.getBytes();
        System.arraycopy(src, 0, keyBytes, 0, Math.min(src.length, size));
        return new SecretKeySpec(keyBytes, "AES");
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(GCM_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, primaryKey, new GCMParameterSpec(TAG_LENGTH, iv));
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
            return decryptInternal(dbData);
        } catch (Exception e) {
            log.error("Falha ao descriptografar dado sensível no banco " +
                      "(corrupção, chave incorreta ou dado não cifrado). Tamanho: {} caracteres",
                      dbData.length(), e);
            throw new RuntimeException("Erro ao descriptografar dado sensível — verifique a chave de criptografia", e);
        }
    }

    /**
     * AUD-02: tolerant variant used EXCLUSIVELY by migration runners.
     * Returns plaintext as-is when data is not Base64 (pre-encryption legacy).
     * Real decryption failures (wrong key) still throw.
     */
    public String decryptLenient(String dbData) {
        if (dbData == null) return null;
        try {
            return decryptInternal(dbData);
        } catch (IllegalArgumentException e) {
            // Not Base64 → plaintext legacy. Return as-is for re-encryption.
            return dbData;
        } catch (Exception e) {
            throw new RuntimeException("Falha ao descriptografar dado durante a migração", e);
        }
    }

    private String decryptInternal(String dbData) throws Exception {
        if (dbData.startsWith(GCM_PREFIX)) {
            byte[] combined = Base64.getDecoder().decode(dbData.substring(GCM_PREFIX.length()));
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            byte[] ciphertext = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, IV_LENGTH, ciphertext, 0, ciphertext.length);
            return tryGcmKeys(iv, ciphertext);
        }
        // Legacy ECB path. Base64.decode throws IllegalArgumentException for plaintext.
        return tryEcbKeys(dbData);
    }

    private String tryGcmKeys(byte[] iv, byte[] ciphertext) throws Exception {
        Exception last = null;
        for (SecretKeySpec key : allGcmKeys) {
            try {
                Cipher cipher = Cipher.getInstance(GCM_ALGORITHM);
                cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH, iv));
                return new String(cipher.doFinal(ciphertext));
            } catch (Exception e) {
                last = e;
            }
        }
        throw new RuntimeException("Nenhuma chave GCM conseguiu descriptografar", last);
    }

    private String tryEcbKeys(String dbData) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(dbData); // throws IllegalArgumentException for plaintext
        if (ecbFallbackKeys.isEmpty()) {
            throw new RuntimeException("Nenhuma chave ECB configurada para descriptografar dado legado em Base64");
        }
        Exception last = null;
        for (SecretKeySpec key : ecbFallbackKeys) {
            try {
                Cipher cipher = Cipher.getInstance(ECB_ALGORITHM);
                cipher.init(Cipher.DECRYPT_MODE, key);
                return new String(cipher.doFinal(decoded));
            } catch (Exception e) {
                last = e;
            }
        }
        throw new RuntimeException("Nenhuma chave ECB conseguiu descriptografar", last);
    }

    /**
     * Returns true if the primary key cannot decrypt {@code dbData}, meaning
     * the value needs to be re-encrypted by {@link com.sistema.lucas.config.EncryptionMigrationRunner}.
     */
    public boolean isEncryptedWithOldKey(String dbData) {
        if (dbData == null) return false;
        try {
            if (dbData.startsWith(GCM_PREFIX)) {
                byte[] combined = Base64.getDecoder().decode(dbData.substring(GCM_PREFIX.length()));
                byte[] iv = new byte[IV_LENGTH];
                System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
                byte[] ciphertext = new byte[combined.length - IV_LENGTH];
                System.arraycopy(combined, IV_LENGTH, ciphertext, 0, ciphertext.length);
                try {
                    Cipher cipher = Cipher.getInstance(GCM_ALGORITHM);
                    cipher.init(Cipher.DECRYPT_MODE, primaryKey, new GCMParameterSpec(TAG_LENGTH, iv));
                    cipher.doFinal(ciphertext);
                    return false; // Primary key succeeded → already on current key
                } catch (javax.crypto.AEADBadTagException e) {
                    return true; // Primary key failed → needs re-encryption
                }
            }
            return true; // ECB or plaintext → needs migration
        } catch (IllegalArgumentException e) {
            return true; // Not Base64 → plaintext legacy
        } catch (Exception e) {
            return false;
        }
    }
}
