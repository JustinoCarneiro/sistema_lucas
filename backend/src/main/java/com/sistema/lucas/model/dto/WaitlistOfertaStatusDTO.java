package com.sistema.lucas.model.dto;

public record WaitlistOfertaStatusDTO(
    boolean valido,
    boolean jaConfirmada,
    boolean expirada,
    String profissionalNome,
    String dataHora
) {}
