package com.sistema.lucas.model.dto;

import jakarta.validation.constraints.NotBlank;

public record WaitlistTokenDTO(
    @NotBlank String token
) {}
