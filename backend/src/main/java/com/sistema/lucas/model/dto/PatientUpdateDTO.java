// backend/src/main/java/com/sistema/lucas/model/dto/PatientUpdateDTO.java
package com.sistema.lucas.model.dto;

public record PatientUpdateDTO(
    String phone,
    String insurance,
    String newPassword
) {}