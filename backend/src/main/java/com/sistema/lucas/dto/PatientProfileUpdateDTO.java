package com.sistema.lucas.dto;

public record PatientProfileUpdateDTO(
        String whatsapp, 
        String newPassword // Opcional: só preenche se quiser mudar a senha
) {}