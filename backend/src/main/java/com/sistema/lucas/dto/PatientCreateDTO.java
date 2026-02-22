package com.sistema.lucas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.br.CPF; // <--- Agora vai ficar usada!

public record PatientCreateDTO(
    @NotBlank(message = "Nome é obrigatório") 
    String name,

    @NotBlank 
    @Email(message = "Email inválido") 
    String email,

    @NotBlank 
    String password,

    @NotBlank(message = "CPF é obrigatório")
    @CPF(message = "CPF inválido") // <--- AQUI ESTÁ O USO
    String cpf,

    String healthInsurance,

    @NotBlank(message = "O WhatsApp é obrigatório para notificações")
    String whatsapp
) {}