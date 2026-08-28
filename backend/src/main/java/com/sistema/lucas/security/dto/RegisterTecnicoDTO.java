package com.sistema.lucas.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// POST /auth/registrar-tecnico — só ADMIN chama isso (@PreAuthorize no controller). Técnico não
// é Patient nem Professional, não tem CPF/perfil clínico — só um User puro, igual o ADMIN
// fundador (AdminInitializer), só que criado sob demanda em vez de bootstrap por env var.
public record RegisterTecnicoDTO(
    @NotBlank String name,
    @Email @NotBlank String email,
    @NotBlank @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres.") String password
) {}
