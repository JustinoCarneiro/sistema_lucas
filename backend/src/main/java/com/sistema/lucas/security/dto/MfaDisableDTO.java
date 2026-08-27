package com.sistema.lucas.security.dto;

import jakarta.validation.constraints.NotBlank;

public record MfaDisableDTO(@NotBlank String password, @NotBlank String code) {}
