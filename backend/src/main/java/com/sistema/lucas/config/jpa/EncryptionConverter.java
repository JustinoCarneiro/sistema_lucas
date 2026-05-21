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
import java.util.Base64;

@Component
@Converter
public class EncryptionConverter implements AttributeConverter<String, String> {

    private static final Logger log = LoggerFactory.getLogger(EncryptionConverter.class);
    private static final String GCM_ALGORITHM = "AES/GCM/NoPadding";
    private static final String ECB_ALGORITHM = "AES/ECB/PKCS5Padding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;
    private static final String GCM_PREFIX = "GCM:";
    private final SecretKeySpec newKey;
    private final SecretKeySpec oldKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public EncryptionConverter(
            @Value("${api.security.encryption.key}") String encryptionKey,
            @Value("${api.security.encryption.key.old}") String oldEncryptionKey) {
            
        // Chave Nova: AES-256 (32 bytes)
        byte[] newKeyBytes = new byte[32];
        byte[] providedNewBytes = encryptionKey.getBytes();
        System.arraycopy(providedNewBytes, 0, newKeyBytes, 0, Math.min(providedNewBytes.length, 32));
        this.newKey = new SecretKeySpec(newKeyBytes, "AES");

        // Chave Antiga: AES-128 (16 bytes)
        byte[] oldKeyBytes = new byte[16];
        byte[] providedOldBytes = oldEncryptionKey.getBytes();
        System.arraycopy(providedOldBytes, 0, oldKeyBytes, 0, Math.min(providedOldBytes.length, 16));
        this.oldKey = new SecretKeySpec(oldKeyBytes, "AES");
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(GCM_ALGORITHM);
            // AUD-01: Sempre encriptar com a nova chave AES-256
            cipher.init(Cipher.ENCRYPT_MODE, newKey, new GCMParameterSpec(TAG_LENGTH, iv));
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
            // AUD-02: nunca devolver o dado bruto silenciosamente — falha de
            // descriptografia é tratada como erro explícito e auditável.
            log.error("Falha ao descriptografar dado sensível no banco " +
                      "(corrupção, chave incorreta ou dado não cifrado). Tamanho: {} caracteres",
                      dbData.length(), e);
            throw new RuntimeException("Erro ao descriptografar dado sensível — verifique a chave de criptografia", e);
        }
    }

    /**
     * AUD-02: variante tolerante usada EXCLUSIVAMENTE pelos runners de migração,
     * que precisam ler dados legados em texto plano (pré-criptografia) para
     * recifrá-los. Falhas criptográficas reais (chave incorreta) continuam
     * lançando erro — apenas o caso "não é Base64 = texto plano" é tolerado.
     */
    public String decryptLenient(String dbData) {
        if (dbData == null) return null;
        try {
            return decryptInternal(dbData);
        } catch (IllegalArgumentException e) {
            // Não é Base64 → texto plano legado. Devolve como está para recifragem.
            return dbData;
        } catch (Exception e) {
            throw new RuntimeException("Falha ao descriptografar dado durante a migração", e);
        }
    }

    // Núcleo de descriptografia: GCM (AES-256 → AES-128) ou ECB legado.
    private String decryptInternal(String dbData) throws Exception {
        // Formato novo: AES/GCM com IV embutido
        if (dbData.startsWith(GCM_PREFIX)) {
            byte[] combined = Base64.getDecoder().decode(dbData.substring(GCM_PREFIX.length()));
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            byte[] ciphertext = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, IV_LENGTH, ciphertext, 0, ciphertext.length);
            try {
                // Tenta AES-256 primeiro
                Cipher cipher = Cipher.getInstance(GCM_ALGORITHM);
                cipher.init(Cipher.DECRYPT_MODE, newKey, new GCMParameterSpec(TAG_LENGTH, iv));
                return new String(cipher.doFinal(ciphertext));
            } catch (javax.crypto.AEADBadTagException e) {
                // AUD-01: Fallback para AES-128 se a tag de autenticação falhar
                Cipher cipher = Cipher.getInstance(GCM_ALGORITHM);
                cipher.init(Cipher.DECRYPT_MODE, oldKey, new GCMParameterSpec(TAG_LENGTH, iv));
                return new String(cipher.doFinal(ciphertext));
            }
        }
        // Formato legado: AES/ECB
        Cipher cipher = Cipher.getInstance(ECB_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, oldKey);
        return new String(cipher.doFinal(Base64.getDecoder().decode(dbData)));
    }

    // Utilitário para o Batch Migration Runner saber se precisa forçar UPDATE
    public boolean isEncryptedWithOldKey(String dbData) {
        if (dbData == null) return false;
        try {
            if (dbData.startsWith(GCM_PREFIX)) {
                byte[] combined = Base64.getDecoder().decode(dbData.substring(GCM_PREFIX.length()));
                byte[] iv = new byte[IV_LENGTH];
                System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
                byte[] ciphertext = new byte[combined.length - IV_LENGTH];
                System.arraycopy(combined, IV_LENGTH, ciphertext, 0, ciphertext.length);
                
                Cipher cipher = Cipher.getInstance(GCM_ALGORITHM);
                cipher.init(Cipher.DECRYPT_MODE, newKey, new GCMParameterSpec(TAG_LENGTH, iv));
                cipher.doFinal(ciphertext);
                return false; // Decifrou com a nova = Não precisa migrar
            }
            return true; // É ECB legado = Precisa migrar
        } catch (javax.crypto.AEADBadTagException e) {
            return true; // Falhou GCM com a nova = Precisa migrar
        } catch (IllegalArgumentException e) {
            return true; // É texto plano legado (não é Base64) = Precisa migrar
        } catch (Exception e) {
            return false;
        }
    }
}
