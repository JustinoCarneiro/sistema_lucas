// backend/src/main/java/com/sistema/lucas/security/dto/RegisterDTO.java
package com.sistema.lucas.security.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterDTO(
    @NotBlank String name,
    @Email @NotBlank String email,
    @NotBlank String password,
    String cpf,
    String phone,
    // LGPD — consentimento expresso obrigatório. O cadastro só prossegue se true.
    @AssertTrue(message = "É necessário aceitar os Termos de Uso e a Política de Privacidade.")
    boolean termsAccepted
    // role removido — fixado como PATIENT no controller, sem risco de escalada
) {}
