package com.sistema.lucas.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record AppointmentCreateDTO(
    @NotNull Long doctorId,
    @NotNull Long patientId,
    @NotNull @Future LocalDateTime startTime,
    @NotNull @Future LocalDateTime endTime,
    String reason
) {
    // Validação extra: O fim não pode ser antes do começo
    // (Podemos fazer isso no Service para simplificar agora)
}