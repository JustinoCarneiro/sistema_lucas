package com.sistema.lucas.security.config;

import com.sistema.lucas.model.infra.ExceptionDTO;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 1. Quando não há corpo no retorno, usamos <Void>
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Void> handle404(EntityNotFoundException e) {
        logger.warn("Entidade não encontrada: {}", e.getMessage());
        return ResponseEntity.notFound().build();
    }

    // 2. Quando retornamos o DTO de erro, usamos <ExceptionDTO>
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ExceptionDTO> handleDataIntegrity(DataIntegrityViolationException exception) {
        logger.error("Erro de integridade de dados: ", exception);
        ExceptionDTO response = new ExceptionDTO("Erro de integridade: dado já cadastrado.", "400");
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ExceptionDTO> handleRuntime(RuntimeException exception) {
        logger.error("Erro de runtime: ", exception);
        ExceptionDTO response = new ExceptionDTO(exception.getMessage(), "400");
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionDTO> handleValidation(org.springframework.web.bind.MethodArgumentNotValidException exception) {
        logger.error("Erro de validação de argumentos: ", exception);
        String details = exception.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + " " + f.getDefaultMessage())
            .collect(java.util.stream.Collectors.joining(", "));
        ExceptionDTO response = new ExceptionDTO("Erro de validação: " + details, "400");
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionDTO> handleGeneral(Exception exception) {
        logger.error("Erro interno não tratado: ", exception);
        ExceptionDTO response = new ExceptionDTO("Erro interno no servidor.", "500");
        return ResponseEntity.internalServerError().body(response);
    }

    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ResponseEntity<ExceptionDTO> handleBadCredentials() {
        logger.warn("Tentativa de login com credenciais inválidas.");
        ExceptionDTO response = new ExceptionDTO("E-mail ou senha inválidos", "401");
        return ResponseEntity.status(401).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ExceptionDTO> handleAccessDenied(AccessDeniedException e) {
        logger.warn("Acesso negado: {}", e.getMessage());
        ExceptionDTO response = new ExceptionDTO("Você não tem permissão para realizar esta operação.", "403");
        return ResponseEntity.status(403).body(response);
    }
}