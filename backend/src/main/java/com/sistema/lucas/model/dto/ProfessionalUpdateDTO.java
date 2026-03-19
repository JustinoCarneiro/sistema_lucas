// backend/src/main/java/com/sistema/lucas/model/dto/ProfessionalUpdateDTO.java
package com.sistema.lucas.model.dto;

import com.sistema.lucas.model.enums.TipoRegistro;

public record ProfessionalUpdateDTO(
    String name,
    String email,
    TipoRegistro tipoRegistro,
    String registroConselho,
    String specialty,
    String cpf,
    String phone,
    java.time.LocalDate birthDate,
    String gender,
    String address,
    String newPassword
) {}