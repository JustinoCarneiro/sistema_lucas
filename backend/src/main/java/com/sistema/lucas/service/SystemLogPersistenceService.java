// backend/src/main/java/com/sistema/lucas/service/SystemLogPersistenceService.java
package com.sistema.lucas.service;

import com.sistema.lucas.logging.InMemoryLogAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Drena a fila em memória do {@link InMemoryLogAppender} periodicamente e persiste em lote em
 * `system_logs` — alimenta GET /system-logs (ADMIN/TECNICO, ver /panel/logs). Também expurga
 * registros com mais de 30 dias (retenção).
 *
 * Todo o corpo dos dois jobs roda protegido por try/catch amplo, de propósito: se o INSERT/DELETE
 * falhar (ex.: banco fora do ar), o evento é só descartado — nunca deixamos essa falha virar um
 * novo log de erro, senão a captura de log entraria num loop tentando logar a própria falha de
 * logar.
 */
@Service
public class SystemLogPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(SystemLogPersistenceService.class);
    private static final int RETENCAO_DIAS = 30;

    @Autowired private JdbcTemplate jdbcTemplate;

    @Scheduled(fixedRate = 10 * 1000)
    public void persistirLogsPendentes() {
        try {
            List<InMemoryLogAppender.CapturedLogEvent> batch = InMemoryLogAppender.drain(500);
            if (batch.isEmpty()) return;

            List<Object[]> params = batch.stream()
                .map(e -> new Object[]{e.level(), e.loggerName(), e.message(), e.stackTrace(), e.criadoEm()})
                .toList();

            jdbcTemplate.batchUpdate(
                "INSERT INTO system_logs (level, logger_name, message, stack_trace, criado_em) VALUES (?, ?, ?, ?, ?)",
                params
            );
        } catch (Exception e) {
            // Não usar log.error aqui de propósito — geraria um novo evento WARN/ERROR que a
            // fila tentaria capturar de novo. System.err é aceitável só neste caso específico
            // (falha da própria infraestrutura de captura, não do sistema em si).
            System.err.println("[SystemLogPersistenceService] Falha ao persistir logs (descartando o lote): " + e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 3 * * *") // 03:00 todo dia — fora do horário de uso da clínica
    public void expurgarLogsAntigos() {
        try {
            LocalDateTime limite = LocalDateTime.now().minusDays(RETENCAO_DIAS);
            int removidos = jdbcTemplate.update("DELETE FROM system_logs WHERE criado_em < ?", limite);
            if (removidos > 0) {
                log.info("[SystemLogPersistenceService] Expurgados {} logs com mais de {} dias.", removidos, RETENCAO_DIAS);
            }
        } catch (Exception e) {
            System.err.println("[SystemLogPersistenceService] Falha ao expurgar logs antigos: " + e.getMessage());
        }
    }
}
