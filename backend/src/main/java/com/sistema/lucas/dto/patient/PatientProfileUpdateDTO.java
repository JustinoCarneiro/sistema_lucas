package com.sistema.lucas.dto.patient;

public record PatientProfileUpdateDTO(
        String whatsapp, 
        String newPassword // Opcional: só preenche se quiser mudar a senha
) {}