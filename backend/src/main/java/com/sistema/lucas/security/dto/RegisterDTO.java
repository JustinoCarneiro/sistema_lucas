// backend/src/main/java/com/sistema/lucas/security/dto/RegisterDTO.java
package com.sistema.lucas.security.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterDTO(
    @NotBlank String name,
    @Email @NotBlank String email,
    // Mesmo mínimo já exigido em /auth/redefinir-senha (PasswordResetController) — o front já
    // aplica essa regra (register.ts, Validators.minLength(6)), isso é só a mesma trava no
    // backend, pra quem chamar a API direto sem passar pela UI.
    @NotBlank @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres.") String password,
    String cpf,
    String phone,
    // LGPD — consentimento expresso obrigatório. O cadastro só prossegue se true.
    @AssertTrue(message = "É necessário aceitar os Termos de Uso e a Política de Privacidade.")
    boolean termsAccepted
    // role removido — fixado como PATIENT no controller, sem risco de escalada
) {}
