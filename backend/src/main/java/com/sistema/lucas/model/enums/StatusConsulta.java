package com.sistema.lucas.model.enums;

public enum StatusConsulta {
    AGUARDANDO_CONFIRMACAO,   // Estado inicial: aguarda aprovação do profissional
    AGENDADA,
    CONFIRMADA_PROFISSIONAL,  // Profissional confirma primeiro
    CONFIRMADA,               // Paciente confirma depois → totalmente confirmada
    CONCLUIDA,
    CANCELADA,
    FALTA
}