package com.sistema.lucas.dto;

import com.sistema.lucas.domain.Doctor;

public record DoctorResponseDTO(
        Long id,
        String name,
        String email,
        String crm,
        String specialty,
        Boolean active
) {

    public DoctorResponseDTO(Doctor doctor) {
        this(
                doctor.getId(),
                doctor.getName(),
                doctor.getEmail(),
                doctor.getCrm(),
                doctor.getSpecialty(),
                doctor.getActive()
        );
    }
}

