package com.sistema.lucas.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProntuarioCreateDTO(
    @NotNull Long appointmentId,
    @NotBlank String notas
) {}
