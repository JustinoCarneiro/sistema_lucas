package com.sistema.lucas.dto;

public record PatientProfileDTO(
        String name, 
        String email, 
        String cpf, 
        String whatsapp
) {}