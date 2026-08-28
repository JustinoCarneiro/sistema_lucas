package com.sistema.lucas.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.sistema.lucas.logging.InMemoryLogAppender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class SystemLogPersistenceServiceTest {

    @InjectMocks private SystemLogPersistenceService service;

    @Mock private JdbcTemplate jdbcTemplate;

    // Empurra um evento de verdade pela mesma porta de entrada que o Logback usa
    // (doAppend, público, herdado de AppenderBase — chama append() por baixo) em vez de
    // acessar a fila estática via reflection.
    private void pushEvent(String level, String loggerName, String message) {
        var appender = new InMemoryLogAppender();
        appender.start();

        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getLevel()).thenReturn(Level.toLevel(level));
        when(event.getLoggerName()).thenReturn(loggerName);
        when(event.getFormattedMessage()).thenReturn(message);
        when(event.getTimeStamp()).thenReturn(System.currentTimeMillis());
        when(event.getThrowableProxy()).thenReturn(null);

        appender.doAppend(event);
    }

    @Nested @DisplayName("persistirLogsPendentes")
    class PersistirTests {

        @Test @DisplayName("Fila vazia não chama batchUpdate")
        void filaVaziaNaoChamaBatchUpdate() {
            InMemoryLogAppender.drain(10_000); // garante estado limpo (fila é estática)

            service.persistirLogsPendentes();

            verify(jdbcTemplate, never()).batchUpdate(anyString(), anyList());
        }

        @Test @DisplayName("Evento na fila é drenado e persistido em lote")
        void eventoNaFilaEhPersistido() {
            InMemoryLogAppender.drain(10_000);
            pushEvent("ERROR", "com.sistema.lucas.service.EmailService", "Falha de SMTP");

            service.persistirLogsPendentes();

            verify(jdbcTemplate).batchUpdate(
                eq("INSERT INTO system_logs (level, logger_name, message, stack_trace, criado_em) VALUES (?, ?, ?, ?, ?)"),
                argThat((List<Object[]> params) -> {
                    if (params.size() != 1) return false;
                    Object[] row = params.get(0);
                    return "ERROR".equals(row[0])
                        && "com.sistema.lucas.service.EmailService".equals(row[1])
                        && "Falha de SMTP".equals(row[2]);
                })
            );

            // Fila drenada — uma segunda chamada não reprocessa o mesmo evento.
            assertTrue(InMemoryLogAppender.drain(10).isEmpty());
        }

        @Test @DisplayName("Falha no batchUpdate não propaga exceção (nunca deixa escapar, senão vira log recursivo)")
        void falhaNoBatchUpdateNaoPropaga() {
            InMemoryLogAppender.drain(10_000);
            pushEvent("WARN", "x", "y");
            when(jdbcTemplate.batchUpdate(anyString(), anyList())).thenThrow(new RuntimeException("banco fora do ar"));

            assertDoesNotThrow(() -> service.persistirLogsPendentes());
        }
    }

    @Nested @DisplayName("expurgarLogsAntigos")
    class ExpurgarTests {

        @Test @DisplayName("Chama DELETE com o corte de 30 dias")
        void chamaDeleteComCorteDe30Dias() {
            when(jdbcTemplate.update(anyString(), any(LocalDateTime.class))).thenReturn(5);

            service.expurgarLogsAntigos();

            verify(jdbcTemplate).update(eq("DELETE FROM system_logs WHERE criado_em < ?"), any(LocalDateTime.class));
        }

        @Test @DisplayName("Falha no DELETE não propaga exceção")
        void falhaNoDeleteNaoPropaga() {
            when(jdbcTemplate.update(anyString(), any(LocalDateTime.class))).thenThrow(new RuntimeException("banco fora do ar"));

            assertDoesNotThrow(() -> service.expurgarLogsAntigos());
        }
    }
}
