package com.sistema.lucas.domain.enums;

public enum AppointmentStatus {
    SCHEDULED,       // Agendado (Marcado)
    CONFIRMED,       // Confirmado (Paciente disse que vai)
    COMPLETED,       // Realizado (Médico atendeu)
    CANCELLED,       // Cancelado
    NO_SHOW          // Faltou (Paciente não apareceu)
}