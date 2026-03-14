package com.sistema.lucas.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;

public record PatientCreateDTO(
    @NotBlank String name, 
    @NotBlank @Email String email, 
    @NotBlank String password, 
    @NotBlank String cpf, 
    String whatsapp, 
    String healthPlan
) {}