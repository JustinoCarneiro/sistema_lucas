package com.sistema.lucas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// Java Record: Imutável, rápido e limpo.
public record DoctorCreateDTO(
    @NotBlank(message = "Nome obrigatório") String name,
    @NotBlank @Email(message = "Email inválido") String email,
    @NotBlank String password,
    @NotBlank String crm,
    @NotBlank String specialty
) {
    // Podemos ter métodos úteis aqui se precisar
}