// backend/src/main/java/com/sistema/lucas/model/dto/PatientResponseDTO.java
package com.sistema.lucas.model.dto;

import java.time.LocalDate;

import com.sistema.lucas.model.Patient;

public record PatientResponseDTO(
    Long id,
    String name,
    String email,
    String cpf,
    String phone,
    LocalDate birthDate,
    String emergencyContactName,
    String emergencyContactPhone,
    String gender,
    String allergies,
    String address
) {
    public PatientResponseDTO(Patient p) {
        this(
            p.getId(),
            p.getName(),
            p.getEmail(),
            p.getCpf(),
            p.getPhone(),
            p.getBirthDate(),
            p.getEmergencyContactName(),
            p.getEmergencyContactPhone(),
            p.getGender(),
            p.getAllergies(),
            p.getAddress()
        );
    }
}