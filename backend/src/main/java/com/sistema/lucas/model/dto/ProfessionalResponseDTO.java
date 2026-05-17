package com.sistema.lucas.model.dto;

import com.sistema.lucas.model.Professional;
import com.sistema.lucas.model.enums.TipoRegistro;

import java.time.LocalDate;

public record ProfessionalResponseDTO(
    Long id,
    String name,
    String email,
    TipoRegistro tipoRegistro,
    String registroConselho,
    String specialty,
    String cpf,
    String phone,
    LocalDate birthDate,
    String gender,
    String address
) {
    public ProfessionalResponseDTO(Professional p) {
        this(
            p.getId(),
            p.getName(),
            p.getEmail(),
            p.getTipoRegistro(),
            p.getRegistroConselho(),
            p.getSpecialty(),
            p.getCpf(),
            p.getPhone(),
            p.getBirthDate(),
            p.getGender(),
            p.getAddress()
        );
    }
}
