package com.sistema.lucas.dto;

import org.springframework.http.HttpStatus;
import java.util.Map;

public record ErrorResponseDTO(
    String message,
    HttpStatus status,
    Map<String, String> errors // Para campos inválidos (ex: email: formato errado)
) {}