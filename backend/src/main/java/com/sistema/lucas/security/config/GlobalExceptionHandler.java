package com.sistema.lucas.security.config;

import com.sistema.lucas.model.infra.ExceptionDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Quando não há corpo no retorno, usamos <Void>
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Void> handle404() {
        return ResponseEntity.notFound().build();
    }

    // 2. Quando retornamos o DTO de erro, usamos <ExceptionDTO>
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ExceptionDTO> handleDataIntegrity(DataIntegrityViolationException exception) {
        ExceptionDTO response = new ExceptionDTO("Erro de integridade: dado já cadastrado.", "400");
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ExceptionDTO> handleRuntime(RuntimeException exception) {
        ExceptionDTO response = new ExceptionDTO(exception.getMessage(), "400");
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionDTO> handleGeneral(Exception exception) {
        ExceptionDTO response = new ExceptionDTO("Erro interno no servidor.", "500");
        return ResponseEntity.internalServerError().body(response);
    }

    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ResponseEntity<ExceptionDTO> handleBadCredentials() {
        ExceptionDTO response = new ExceptionDTO("E-mail ou senha inválidos", "401");
        return ResponseEntity.status(401).body(response);
    }
}