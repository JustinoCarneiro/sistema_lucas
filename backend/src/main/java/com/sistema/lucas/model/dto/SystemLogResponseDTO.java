package com.sistema.lucas.model.dto;

import com.sistema.lucas.model.SystemLog;

import java.time.LocalDateTime;

public record SystemLogResponseDTO(
    Long id,
    String level,
    String loggerName,
    String message,
    String stackTrace,
    LocalDateTime criadoEm
) {
    public SystemLogResponseDTO(SystemLog log) {
        this(log.getId(), log.getLevel(), log.getLoggerName(), log.getMessage(), log.getStackTrace(), log.getCriadoEm());
    }
}
