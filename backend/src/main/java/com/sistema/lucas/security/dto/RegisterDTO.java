// backend/src/main/java/com/sistema/lucas/security/dto/RegisterDTO.java
package com.sistema.lucas.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterDTO(
    @NotBlank String name,
    @Email @NotBlank String email,
    @NotBlank String password,
    String cpf,
    String phone
    // role removido — fixado como PATIENT no controller, sem risco de escalada
) {}