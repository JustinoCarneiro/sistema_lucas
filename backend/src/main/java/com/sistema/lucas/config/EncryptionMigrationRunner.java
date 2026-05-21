package com.sistema.lucas.config;

import com.sistema.lucas.config.jpa.EncryptionConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class EncryptionMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(EncryptionMigrationRunner.class);

    private final JdbcTemplate jdbcTemplate;
    private final EncryptionConverter encryptionConverter;

    public EncryptionMigrationRunner(JdbcTemplate jdbcTemplate, EncryptionConverter encryptionConverter) {
        this.jdbcTemplate = jdbcTemplate;
        this.encryptionConverter = encryptionConverter;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("[AUD-01] Iniciando verificação de Batch Migration das chaves AES...");

        int totalMigrated = 0;

        // 1. Users (totp_secret)
        Integer usersMigrated = jdbcTemplate.query("SELECT id, totp_secret FROM users WHERE totp_secret IS NOT NULL", rs -> {
            int count = 0;
            while (rs.next()) {
                String encryptedVal = rs.getString("totp_secret");
                if (encryptionConverter.isEncryptedWithOldKey(encryptedVal)) {
                    String decrypted = encryptionConverter.convertToEntityAttribute(encryptedVal);
                    String reEncrypted = encryptionConverter.convertToDatabaseColumn(decrypted);
                    jdbcTemplate.update("UPDATE users SET totp_secret = ? WHERE id = ?", reEncrypted, rs.getLong("id"));
                    count++;
                }
            }
            return count;
        });
        totalMigrated += (usersMigrated != null ? usersMigrated : 0);

        // 2. Patient
        Integer patientsMigrated = jdbcTemplate.query("SELECT id, cpf, phone, emergency_contact_name, emergency_contact_phone, allergies, address FROM patient", rs -> {
            int count = 0;
            while (rs.next()) {
                String cpf = rs.getString("cpf");
                String phone = rs.getString("phone");
                String eName = rs.getString("emergency_contact_name");
                String ePhone = rs.getString("emergency_contact_phone");
                String allergies = rs.getString("allergies");
                String address = rs.getString("address");

                if (encryptionConverter.isEncryptedWithOldKey(cpf) ||
                    encryptionConverter.isEncryptedWithOldKey(phone) ||
                    encryptionConverter.isEncryptedWithOldKey(eName) ||
                    encryptionConverter.isEncryptedWithOldKey(ePhone) ||
                    encryptionConverter.isEncryptedWithOldKey(allergies) ||
                    encryptionConverter.isEncryptedWithOldKey(address)) {

                    String mCpf = encryptionConverter.convertToDatabaseColumn(encryptionConverter.convertToEntityAttribute(cpf));
                    String mPhone = encryptionConverter.convertToDatabaseColumn(encryptionConverter.convertToEntityAttribute(phone));
                    String mEName = encryptionConverter.convertToDatabaseColumn(encryptionConverter.convertToEntityAttribute(eName));
                    String mEPhone = encryptionConverter.convertToDatabaseColumn(encryptionConverter.convertToEntityAttribute(ePhone));
                    String mAllergies = encryptionConverter.convertToDatabaseColumn(encryptionConverter.convertToEntityAttribute(allergies));
                    String mAddress = encryptionConverter.convertToDatabaseColumn(encryptionConverter.convertToEntityAttribute(address));

                    jdbcTemplate.update("UPDATE patient SET cpf = ?, phone = ?, emergency_contact_name = ?, emergency_contact_phone = ?, allergies = ?, address = ? WHERE id = ?",
                            mCpf, mPhone, mEName, mEPhone, mAllergies, mAddress, rs.getLong("id"));
                    count++;
                }
            }
            return count;
        });
        totalMigrated += (patientsMigrated != null ? patientsMigrated : 0);

        // 3. Professional
        Integer professionalsMigrated = jdbcTemplate.query("SELECT id, cpf, phone, address FROM professional", rs -> {
            int count = 0;
            while (rs.next()) {
                String cpf = rs.getString("cpf");
                String phone = rs.getString("phone");
                String address = rs.getString("address");

                if (encryptionConverter.isEncryptedWithOldKey(cpf) ||
                    encryptionConverter.isEncryptedWithOldKey(phone) ||
                    encryptionConverter.isEncryptedWithOldKey(address)) {

                    String mCpf = encryptionConverter.convertToDatabaseColumn(encryptionConverter.convertToEntityAttribute(cpf));
                    String mPhone = encryptionConverter.convertToDatabaseColumn(encryptionConverter.convertToEntityAttribute(phone));
                    String mAddress = encryptionConverter.convertToDatabaseColumn(encryptionConverter.convertToEntityAttribute(address));

                    jdbcTemplate.update("UPDATE professional SET cpf = ?, phone = ?, address = ? WHERE id = ?",
                            mCpf, mPhone, mAddress, rs.getLong("id"));
                    count++;
                }
            }
            return count;
        });
        totalMigrated += (professionalsMigrated != null ? professionalsMigrated : 0);

        if (totalMigrated > 0) {
            log.info("[AUD-01] Batch Migration concluída! {} registros foram recifrados para AES-256.", totalMigrated);
        } else {
            log.info("[AUD-01] Nenhum registro pendente de migração. Banco já está operando em AES-256.");
        }
    }
}
