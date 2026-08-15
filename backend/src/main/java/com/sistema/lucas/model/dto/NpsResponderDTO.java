package com.sistema.lucas.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NpsResponderDTO(
    @NotBlank String token,
    @NotNull @Min(0) @Max(10) Integer score,
    String comentario
) {}
