package com.sistema.lucas.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;

public record ProfessionalCreateDTO(
    @NotBlank String name, 
    @NotBlank @Email String email, 
    @NotBlank String password, 
    @NotBlank String crm, 
    @NotBlank String specialty
) {}