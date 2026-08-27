package com.sistema.lucas.security.dto;

import jakarta.validation.constraints.NotBlank;

public record MfaCodeDTO(@NotBlank String code) {}
