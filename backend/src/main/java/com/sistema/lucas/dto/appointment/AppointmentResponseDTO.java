package com.sistema.lucas.dto.appointment;

import com.sistema.lucas.domain.enums.AppointmentStatus;
import java.time.LocalDateTime;

public record AppointmentResponseDTO(
    Long id,
    String professionalName,
    String patientName,
    LocalDateTime startTime,
    LocalDateTime endTime,
    AppointmentStatus status
) {}