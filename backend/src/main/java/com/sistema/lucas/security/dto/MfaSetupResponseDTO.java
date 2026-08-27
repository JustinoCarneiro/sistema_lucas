package com.sistema.lucas.security.dto;

public record MfaSetupResponseDTO(String secretBase32, String otpAuthUri) {}
