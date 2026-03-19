// backend/src/main/java/com/sistema/lucas/model/dto/PatientUpdateDTO.java
package com.sistema.lucas.model.dto;

import java.time.LocalDate;

public record PatientUpdateDTO(
    String name,
    String email,
    String cpf,
    String phone,
    String newPassword,
    LocalDate birthDate,
    String emergencyContactName,
    String emergencyContactPhone,
    String gender,
    String allergies,
    String address
) {}