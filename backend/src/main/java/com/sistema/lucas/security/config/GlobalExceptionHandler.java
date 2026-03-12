package com.sistema.lucas.security.config;

import com.sistema.lucas.model.infra.ExceptionDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Captura erros de Entidade não encontrada (ex: findById que não existe)
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity handle404() {
        return ResponseEntity.notFound().build();
    }

    // Captura erros de integridade (ex: CPF/CRM duplicado no banco)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity handleDataIntegrity(DataIntegrityViolationException exception) {
        ExceptionDTO response = new ExceptionDTO("Erro de integridade: dado já cadastrado.", "400");
        return ResponseEntity.badRequest().body(response);
    }

    // Captura as RuntimeExceptions que lançamos nos nossos Services
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity handleRuntime(RuntimeException exception) {
        ExceptionDTO response = new ExceptionDTO(exception.getMessage(), "400");
        return ResponseEntity.badRequest().body(response);
    }

    // Captura qualquer outro erro inesperado (Fallback)
    @ExceptionHandler(Exception.class)
    public ResponseEntity handleGeneral(Exception exception) {
        ExceptionDTO response = new ExceptionDTO("Erro interno no servidor.", "500");
        return ResponseEntity.internalServerError().body(response);
    }
}