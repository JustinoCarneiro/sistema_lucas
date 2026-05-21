package com.sistema.lucas.model.dto;

import com.sistema.lucas.model.enums.ModalidadeAtendimento;
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
    String newPassword,
    ModalidadeAtendimento modalidadeAtendimento
) {}
