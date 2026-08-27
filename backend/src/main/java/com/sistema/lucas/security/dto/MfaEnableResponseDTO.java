package com.sistema.lucas.security.dto;

import java.util.List;

// backupCodes: só existem em texto plano nesta resposta — depois disso só o hash fica salvo.
public record MfaEnableResponseDTO(List<String> backupCodes) {}
