package com.sistema.lucas.exception;

import com.sistema.lucas.dto.ErrorResponseDTO;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.persistence.EntityNotFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Erro 1: Validação de Campos (@NotBlank, @Email, etc)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponseDTO("Erro de validação", HttpStatus.BAD_REQUEST, errors));
    }

    /**
     * UNIFICADO: Trata erros de integridade do Banco de Dados
     * Resolve o conflito de métodos ambíguos e diferencia Delete de Duplicidade
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleDataIntegrity(DataIntegrityViolationException ex) {
        String message = "Erro de integridade de dados.";
        
        // Captura a mensagem específica do banco (Postgres) para decidir o texto
        String detail = ex.getMostSpecificCause().getMessage().toLowerCase();

        if (detail.contains("violates foreign key") || detail.contains("fk")) {
            // Caso de tentativa de DELETE de médico/paciente com consultas
            message = "Não é possível excluir: este registro possui vínculos (consultas ou exames) ativos.";
        } else if (detail.contains("duplicate key") || detail.contains("already exists")) {
            // Caso de Cadastro de Email/CPF/CRM repetido
            message = "Conflito: Email, CPF ou CRM já cadastrados no sistema.";
        }

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponseDTO(message, HttpStatus.CONFLICT, null));
    }

    // Erro 3: Recurso não encontrado (404)
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponseDTO(ex.getMessage(), HttpStatus.NOT_FOUND, null));
    }

    // Erro 4: Regras de Negócio ou Argumentos Inválidos
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleBusinessRules(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponseDTO(ex.getMessage(), HttpStatus.BAD_REQUEST, null));
    }
    
    // Erro Genérico (Fallback)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGeneral(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponseDTO("Erro interno no servidor: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, null));
    }
}