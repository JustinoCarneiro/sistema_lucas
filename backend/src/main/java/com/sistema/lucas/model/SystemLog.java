// backend/src/main/java/com/sistema/lucas/model/SystemLog.java
package com.sistema.lucas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Entidade só de leitura (a escrita é feita via JdbcTemplate em lote por
// SystemLogPersistenceService, não via este repository/entidade — ver o motivo em
// InMemoryLogAppender). Alimenta GET /system-logs (ADMIN/TECNICO).
@Entity
@Table(name = "system_logs")
@Getter
@Setter
@NoArgsConstructor
public class SystemLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String level;

    @Column(name = "logger_name")
    private String loggerName;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
}
