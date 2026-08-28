// backend/src/main/java/com/sistema/lucas/logging/InMemoryLogAppender.java
package com.sistema.lucas.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.AppenderBase;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Appender do Logback puro (sem Spring — o Logback já está rodando antes do contexto do Spring
 * terminar de subir, então injetar JdbcTemplate aqui seria arriscado). Só empurra cada evento
 * WARN/ERROR numa fila estática em memória; quem persiste de verdade é
 * {@link com.sistema.lucas.service.SystemLogPersistenceService}, um bean Spring normal que drena
 * essa fila periodicamente. Registrado via logback-spring.xml.
 *
 * Fila com teto (5000) — se o banco cair e a fila não for drenada, descarta o mais antigo em vez
 * de crescer sem limite (nunca queremos um vazamento de memória por causa de log).
 */
public class InMemoryLogAppender extends AppenderBase<ILoggingEvent> {

    public record CapturedLogEvent(String level, String loggerName, String message, String stackTrace, LocalDateTime criadoEm) {}

    private static final int MAX_QUEUE_SIZE = 5000;
    private static final ConcurrentLinkedQueue<CapturedLogEvent> QUEUE = new ConcurrentLinkedQueue<>();

    @Override
    protected void append(ILoggingEvent event) {
        // Evita loop: nunca captura os próprios logs do pacote de persistência/appender.
        if (event.getLoggerName() != null && event.getLoggerName().startsWith("com.sistema.lucas.logging")) {
            return;
        }

        String stackTrace = null;
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null) {
            stackTrace = ThrowableProxyUtil.asString(throwableProxy);
        }

        var captured = new CapturedLogEvent(
            event.getLevel().toString(),
            event.getLoggerName(),
            event.getFormattedMessage(),
            stackTrace,
            LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getTimeStamp()), ZoneId.systemDefault())
        );

        QUEUE.add(captured);
        while (QUEUE.size() > MAX_QUEUE_SIZE) {
            QUEUE.poll();
        }
    }

    /** Drena até `max` eventos da fila (usado pelo SystemLogPersistenceService). */
    public static java.util.List<CapturedLogEvent> drain(int max) {
        java.util.List<CapturedLogEvent> batch = new java.util.ArrayList<>();
        CapturedLogEvent event;
        while (batch.size() < max && (event = QUEUE.poll()) != null) {
            batch.add(event);
        }
        return batch;
    }
}
