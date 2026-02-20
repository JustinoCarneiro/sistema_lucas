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

@RestControllerAdvice // <--- Diz ao Spring: "Capture erros de todos os Controllers"
public class GlobalExceptionHandler {

    // Erro 1: Validação de Campos (@NotBlank, @Email, etc)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        
        // Pega cada campo errado e a mensagem
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponseDTO("Erro de validação", HttpStatus.BAD_REQUEST, errors));
    }

    // Erro 2: Violação de Integridade do Banco (Duplicidade)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleDuplicateEntry(DataIntegrityViolationException ex) {
        // O erro do banco vem complexo, simplificamos aqui
        String message = "Conflito de dados: Email, CPF ou CRM já cadastrados.";
        
        return ResponseEntity
                .status(HttpStatus.CONFLICT) // 409 Conflict
                .body(new ErrorResponseDTO(message, HttpStatus.CONFLICT, null));
    }
    
    // Erro Genérico (Fallback)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGeneral(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponseDTO("Erro interno no servidor: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, null));
    }

    // Erro 3: Recurso não encontrado (404)
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND) // Retorna 404 corretamente
                .body(new ErrorResponseDTO(ex.getMessage(), HttpStatus.NOT_FOUND, null));
    }

    // Erro 4: Regras de Negócio (Ex: Horário conflitante)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleBusinessRules(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST) // 400 - O usuário mandou dados que ferem a regra
                .body(new ErrorResponseDTO(ex.getMessage(), HttpStatus.BAD_REQUEST, null));
    }
}