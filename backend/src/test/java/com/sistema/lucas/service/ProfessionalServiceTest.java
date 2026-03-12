package com.sistema.lucas.service;

import com.sistema.lucas.model.ProfessionalCreateDTO;
import com.sistema.lucas.repository.ProfessionalRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfessionalServiceTest {

    @InjectMocks
    private ProfessionalService service;

    @Mock
    private ProfessionalRepository repository;

    @Test
    @DisplayName("Deve lançar exceção ao tentar cadastrar CRM já existente")
    void scenario01() {
        // Arrange
        var dto = new ProfessionalCreateDTO("Dr. Lucas", "lucas@email.com", "senha123", "12345", "Psicologia");
        when(repository.existsByCrm("12345")).thenReturn(true);

        // Act & Assert
        var exception = assertThrows(RuntimeException.class, () -> service.create(dto));
        assertEquals("Erro: Este CRM já está cadastrado no sistema.", exception.getMessage());
        
        verify(repository, never()).save(any());
    }
}