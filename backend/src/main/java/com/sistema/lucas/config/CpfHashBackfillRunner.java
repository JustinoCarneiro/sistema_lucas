package com.sistema.lucas.config;

import com.sistema.lucas.config.jpa.EncryptionConverter;
import com.sistema.lucas.service.CpfHashService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * AUD-03 (LGPD): Recalcula o cpf_hash dos pacientes já existentes, migrando-os
 * do SHA-256 puro (algoritmo antigo, embutido na entidade Patient) para o
 * HMAC-SHA256 com pepper gerado pelo {@link CpfHashService}.
 *
 * <p>Necessário porque o {@code EncryptionMigrationRunner} re-cifra o CPF, mas
 * não recalcula o cpf_hash. Sem este backfill, a verificação de CPF duplicado
 * ({@code existsByCpfHash}) falha para pacientes anteriores à migração.
 *
 * <p>Idempotente: após a primeira execução o hash calculado já é igual ao
 * armazenado e nenhum UPDATE é emitido.
 */
@Component
public class CpfHashBackfillRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CpfHashBackfillRunner.class);

    private final JdbcTemplate jdbcTemplate;
    private final EncryptionConverter encryptionConverter;
    private final CpfHashService cpfHashService;

    public CpfHashBackfillRunner(JdbcTemplate jdbcTemplate,
                                 EncryptionConverter encryptionConverter,
                                 CpfHashService cpfHashService) {
        this.jdbcTemplate = jdbcTemplate;
        this.encryptionConverter = encryptionConverter;
        this.cpfHashService = cpfHashService;
    }

    @Override
    public void run(String... args) {
        log.info("[AUD-03] Verificando backfill de cpf_hash (SHA-256 -> HMAC-SHA256)...");

        // Apenas pacientes ativos com CPF: anonimizados têm cpf nulo e
        // cpf_hash sentinela ("anonymized-<id>"), que não deve ser tocado.
        Integer migrated = jdbcTemplate.query(
            "SELECT id, cpf, cpf_hash FROM patient WHERE cpf IS NOT NULL AND is_active = true",
            rs -> {
                int count = 0;
                while (rs.next()) {
                    long id = rs.getLong("id");
                    String storedHash = rs.getString("cpf_hash");

                    String decryptedCpf;
                    try {
                        // decryptLenient: tolera CPF legado em texto plano (independe
                        // da ordem de execução vs. EncryptionMigrationRunner).
                        decryptedCpf = encryptionConverter.decryptLenient(rs.getString("cpf"));
                    } catch (Exception e) {
                        log.error("[AUD-03] Falha ao decifrar CPF do paciente id={} — pulando.", id, e);
                        continue;
                    }

                    String correctHash = cpfHashService.hash(decryptedCpf);
                    if (correctHash != null && !correctHash.equals(storedHash)) {
                        try {
                            jdbcTemplate.update("UPDATE patient SET cpf_hash = ? WHERE id = ?", correctHash, id);
                            count++;
                        } catch (Exception e) {
                            log.error("[AUD-03] Falha ao atualizar cpf_hash do paciente id={} " +
                                      "(possível CPF duplicado na base) — verificar manualmente.", id, e);
                        }
                    }
                }
                return count;
            });

        if (migrated != null && migrated > 0) {
            log.info("[AUD-03] Backfill concluído: {} cpf_hash recalculados para HMAC-SHA256.", migrated);
        } else {
            log.info("[AUD-03] Nenhum cpf_hash pendente. Todos já estão em HMAC-SHA256.");
        }
    }
}
