package com.sistema.lucas.config;

import com.sistema.lucas.config.jpa.EncryptionConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * AUD-01 / AUD-02: Recifra para AES-256 todos os campos sensíveis ainda
 * gravados com a chave antiga (AES-128) ou em texto plano legado.
 *
 * <p>Cobre TODAS as colunas anotadas com {@code @Convert(EncryptionConverter)}:
 * users, patient, professional, prontuarios, documentos e appointments.
 * Garantir cobertura total é pré-requisito do AUD-02 — só assim o
 * {@code convertToEntityAttribute} pode ser estrito sem risco de quebrar
 * leituras de dados legados.
 *
 * <p>Idempotente: usa {@code isEncryptedWithOldKey} para só atualizar o que
 * realmente precisa migrar.
 */
@Component
public class EncryptionMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(EncryptionMigrationRunner.class);

    private final JdbcTemplate jdbcTemplate;
    private final EncryptionConverter encryptionConverter;

    public EncryptionMigrationRunner(JdbcTemplate jdbcTemplate, EncryptionConverter encryptionConverter) {
        this.jdbcTemplate = jdbcTemplate;
        this.encryptionConverter = encryptionConverter;
    }

    /** Recifra um valor (AES-128/texto plano -> AES-256), tolerando dado legado. */
    private String reencrypt(String value) {
        return encryptionConverter.convertToDatabaseColumn(encryptionConverter.decryptLenient(value));
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
                    jdbcTemplate.update("UPDATE users SET totp_secret = ? WHERE id = ?", reencrypt(encryptedVal), rs.getLong("id"));
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

                    jdbcTemplate.update("UPDATE patient SET cpf = ?, phone = ?, emergency_contact_name = ?, emergency_contact_phone = ?, allergies = ?, address = ? WHERE id = ?",
                            reencrypt(cpf), reencrypt(phone), reencrypt(eName), reencrypt(ePhone), reencrypt(allergies), reencrypt(address), rs.getLong("id"));
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

                    jdbcTemplate.update("UPDATE professional SET cpf = ?, phone = ?, address = ? WHERE id = ?",
                            reencrypt(cpf), reencrypt(phone), reencrypt(address), rs.getLong("id"));
                    count++;
                }
            }
            return count;
        });
        totalMigrated += (professionalsMigrated != null ? professionalsMigrated : 0);

        // 4. Prontuarios (notas clínicas)
        Integer prontuariosMigrated = jdbcTemplate.query("SELECT id, notas FROM prontuarios", rs -> {
            int count = 0;
            while (rs.next()) {
                String notas = rs.getString("notas");
                if (encryptionConverter.isEncryptedWithOldKey(notas)) {
                    jdbcTemplate.update("UPDATE prontuarios SET notas = ? WHERE id = ?", reencrypt(notas), rs.getLong("id"));
                    count++;
                }
            }
            return count;
        });
        totalMigrated += (prontuariosMigrated != null ? prontuariosMigrated : 0);

        // 5. Documentos (conteúdo de texto e PDF em Base64)
        Integer documentosMigrated = jdbcTemplate.query("SELECT id, conteudo_texto, arquivo_base64 FROM documentos", rs -> {
            int count = 0;
            while (rs.next()) {
                String conteudo = rs.getString("conteudo_texto");
                String arquivo = rs.getString("arquivo_base64");
                if (encryptionConverter.isEncryptedWithOldKey(conteudo) ||
                    encryptionConverter.isEncryptedWithOldKey(arquivo)) {
                    jdbcTemplate.update("UPDATE documentos SET conteudo_texto = ?, arquivo_base64 = ? WHERE id = ?",
                            reencrypt(conteudo), reencrypt(arquivo), rs.getLong("id"));
                    count++;
                }
            }
            return count;
        });
        totalMigrated += (documentosMigrated != null ? documentosMigrated : 0);

        // 6. Appointments (motivo da consulta e motivo de cancelamento)
        Integer appointmentsMigrated = jdbcTemplate.query("SELECT id, reason, cancel_reason FROM appointments", rs -> {
            int count = 0;
            while (rs.next()) {
                String reason = rs.getString("reason");
                String cancelReason = rs.getString("cancel_reason");
                if (encryptionConverter.isEncryptedWithOldKey(reason) ||
                    encryptionConverter.isEncryptedWithOldKey(cancelReason)) {
                    jdbcTemplate.update("UPDATE appointments SET reason = ?, cancel_reason = ? WHERE id = ?",
                            reencrypt(reason), reencrypt(cancelReason), rs.getLong("id"));
                    count++;
                }
            }
            return count;
        });
        totalMigrated += (appointmentsMigrated != null ? appointmentsMigrated : 0);

        if (totalMigrated > 0) {
            log.info("[AUD-01] Batch Migration concluída! {} registros foram recifrados para AES-256.", totalMigrated);
        } else {
            log.info("[AUD-01] Nenhum registro pendente de migração. Banco já está operando em AES-256.");
        }
    }
}
