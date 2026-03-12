package com.sistema.lucas.security.dto;

import com.sistema.lucas.model.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterDTO(
    @NotBlank(message = "O email é obrigatório") 
    @Email(message = "Formato de email inválido") 
    String email, 

    @NotBlank(message = "A password é obrigatória") 
    String password, 

    @NotNull(message = "O perfil (role) é obrigatório") 
    Role role
) {}