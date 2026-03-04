package com.sistema.lucas.dto.professional;

import com.sistema.lucas.domain.Professional;

public record ProfessionalResponseDTO(
        Long id,
        String name,
        String email,
        String crm,
        String specialty,
        Boolean active
) {

    public ProfessionalResponseDTO(Professional professional) {
        this(
                professional.getId(),
                professional.getName(),
                professional.getEmail(),
                professional.getCrm(),
                professional.getSpecialty(),
                professional.getActive()
        );
    }
}

