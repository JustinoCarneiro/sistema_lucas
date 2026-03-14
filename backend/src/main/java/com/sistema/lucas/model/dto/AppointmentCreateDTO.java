package com.sistema.lucas.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Future;
import java.time.LocalDateTime;

public record AppointmentCreateDTO(
    @NotNull Long professionalId,
    @NotNull Long patientId,
    @NotNull @Future LocalDateTime dateTime, // Garante que a data seja no futuro
    String reason
) {}