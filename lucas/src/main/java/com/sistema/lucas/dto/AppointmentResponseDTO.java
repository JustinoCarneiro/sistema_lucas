package com.sistema.lucas.dto;

import com.sistema.lucas.domain.enums.AppointmentStatus;
import java.time.LocalDateTime;

public record AppointmentResponseDTO(
    Long id,
    String doctorName,
    String patientName,
    LocalDateTime startTime,
    LocalDateTime endTime,
    AppointmentStatus status
) {}