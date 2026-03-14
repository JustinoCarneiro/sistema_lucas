// backend/src/main/java/com/sistema/lucas/model/dto/AppointmentResponseDTO.java
package com.sistema.lucas.model.dto;

import com.sistema.lucas.model.Appointment;
import java.time.LocalDateTime;

public record AppointmentResponseDTO(
    Long id,
    Long patientId,         
    String professionalName,
    String patientName,
    LocalDateTime startTime,
    String reason,
    String status
) {
    public AppointmentResponseDTO(Appointment app) {
        this(
            app.getId(),
            app.getPatient().getId(),
            app.getProfessional().getName(),
            app.getPatient().getName(),
            app.getDateTime(),
            app.getReason(),
            app.getStatus()
        );
    }
}