package com.sistema.lucas.model.enums;

public enum WaitlistStatus {
    AGUARDANDO,  // na fila, esperando o horário abrir
    OFERECIDA,   // horário abriu; vaga reservada pra esse paciente, aguardando confirmação
    CONFIRMADA,  // paciente confirmou — a consulta segue o ciclo normal (aprovação do profissional etc.)
    EXPIRADA,    // não confirmou dentro do prazo — vaga passou pro próximo da fila
    CANCELADA    // paciente saiu da fila voluntariamente, ou foi pulado (ex.: bloqueio por penalidade)
}
