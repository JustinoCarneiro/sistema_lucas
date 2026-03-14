// backend/src/main/java/com/sistema/lucas/model/dto/ProfessionalUpdateDTO.java
package com.sistema.lucas.model.dto;

import com.sistema.lucas.model.enums.TipoRegistro;

public record ProfessionalUpdateDTO(
    TipoRegistro tipoRegistro,
    String registroConselho,
    String specialty,
    String newPassword
) {}