package com.sistema.lucas.model.dto;

import jakarta.validation.constraints.NotBlank;

public record AppointmentCancelDTO(
    @NotBlank(message = "A justificativa é obrigatória")
    String justification
) {}
