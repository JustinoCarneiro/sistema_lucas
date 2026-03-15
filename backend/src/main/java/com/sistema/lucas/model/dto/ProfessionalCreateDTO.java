// backend/src/main/java/com/sistema/lucas/model/dto/ProfessionalCreateDTO.java
package com.sistema.lucas.model.dto;

import com.sistema.lucas.model.enums.TipoRegistro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProfessionalCreateDTO(
    @NotBlank String name,
    @NotBlank String email,
    String password,
    @NotNull  TipoRegistro tipoRegistro,  
    @NotBlank String registroConselho,   
    String specialty
) {}