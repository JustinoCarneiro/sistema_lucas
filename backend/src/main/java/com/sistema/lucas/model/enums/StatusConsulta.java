package com.sistema.lucas.model.enums;

public enum StatusConsulta {
    AGENDADA,
    CONFIRMADA_PROFISSIONAL,  // Profissional confirma primeiro
    CONFIRMADA,               // Paciente confirma depois → totalmente confirmada
    CONCLUIDA,
    CANCELADA,
    FALTA
}