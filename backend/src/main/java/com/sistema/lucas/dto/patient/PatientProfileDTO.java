package com.sistema.lucas.dto.patient;

public record PatientProfileDTO(
        String name, 
        String email, 
        String cpf, 
        String whatsapp
) {}