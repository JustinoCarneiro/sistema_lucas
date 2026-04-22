// backend/src/main/java/com/sistema/lucas/model/dto/AppointmentResponseDTO.java
package com.sistema.lucas.model.dto;

import com.sistema.lucas.model.Appointment;
import com.sistema.lucas.model.enums.StatusConsulta;
import java.time.LocalDateTime;

public record AppointmentResponseDTO(
    Long id,
    Long patientId,
    String professionalName,
    String patientName,
    LocalDateTime startTime,
    String reason,
    StatusConsulta status,
    boolean podeCancelar
) {
    public AppointmentResponseDTO(Appointment app) {
        this(
            app.getId(),
            app.getPatient().getId(),
            app.getProfessional().getName(),
            app.getPatient().getName(),
            app.getDateTime(),
            app.getReason(),
            app.getStatus(),
            // ✅ pode cancelar se faltam mais de 24h E o status permite
            LocalDateTime.now().isBefore(app.getDateTime().minusHours(24))
                && (app.getStatus() == StatusConsulta.AGENDADA
                    || app.getStatus() == StatusConsulta.CONFIRMADA_PROFISSIONAL
                    || app.getStatus() == StatusConsulta.CONFIRMADA)
        );
    }
}