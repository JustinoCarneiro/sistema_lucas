package com.sistema.lucas.security.dto;

// MFA (SEC-02): mfaRequired=true quando o segundo fator ainda não foi conferido — nesse caso
// role/verified vêm null/false, o frontend não deve agir como se a sessão já estivesse valendo
// (o cookie de sessão real só é emitido depois de POST /auth/mfa/verify).
public record LoginResponseDTO(String role, boolean verified, boolean mfaRequired) {
    public LoginResponseDTO(String role, boolean verified) {
        this(role, verified, false);
    }
}
