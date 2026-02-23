package com.sistema.lucas.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record PatientScheduleDTO(
    @NotNull(message = "O médico é obrigatório") 
    Long doctorId,

    @NotNull(message = "A data de início é obrigatória") 
    @Future(message = "A consulta deve ser no futuro") 
    LocalDateTime startTime,

    @NotNull(message = "A data de fim é obrigatória") 
    @Future(message = "A consulta deve ser no futuro") 
    LocalDateTime endTime,

    String reason
) {}