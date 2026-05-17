package com.sistema.lucas.model.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record AppointmentRescheduleDTO(
    @NotNull(message = "A nova data é obrigatória")
    @Future(message = "A data deve ser no futuro")
    LocalDateTime newDateTime,

    @NotBlank(message = "A justificativa é obrigatória")
    String justification
) {}
