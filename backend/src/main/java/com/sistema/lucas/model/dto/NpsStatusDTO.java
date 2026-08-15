package com.sistema.lucas.model.dto;

public record NpsStatusDTO(
    boolean valido,
    boolean jaRespondido,
    boolean expirado,
    String profissionalNome,
    String dataConsulta
) {}
