// backend/src/main/java/com/sistema/lucas/model/dto/PatientResponseDTO.java
package com.sistema.lucas.model.dto;

import com.sistema.lucas.model.Patient;

public record PatientResponseDTO(
    Long id,
    String name,
    String email,
    String cpf,
    String phone,
    String insurance
) {
    public PatientResponseDTO(Patient p) {
        this(
            p.getId(),
            p.getName(),
            p.getEmail(),
            p.getCpf(),
            p.getPhone(),
            p.getInsurance()
        );
    }
}