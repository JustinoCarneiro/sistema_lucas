// backend/src/main/java/com/sistema/lucas/model/dto/ProntuarioResponseDTO.java
package com.sistema.lucas.model.dto;

import com.sistema.lucas.model.Prontuario;
import java.time.LocalDateTime;

public record ProntuarioResponseDTO(
    Long id,
    Long appointmentId,
    String nomePaciente,
    String nomeProfissional,
    String notas,
    LocalDateTime criadoEm
) {
    public ProntuarioResponseDTO(Prontuario p) {
        this(
            p.getId(),
            p.getAppointment() != null ? p.getAppointment().getId() : null,
            p.getPatient().getName(),
            p.getProfessional().getName(),
            p.getNotas(),
            p.getCriadoEm()
        );
    }
}
