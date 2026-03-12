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
    private ProfessionalService professionalService;

    @Mock
    private ProfessionalRepository professionalRepository;

    @Test
    @DisplayName("Não deve cadastrar profissional com CRM duplicado")
    void cadastrarCrmDuplicado() {
        // Arrange (Preparação)
        var dto = new ProfessionalCreateDTO("Dr. House", "house@med.com", "senha123", "12345-SP", "Infectologia");
        when(professionalRepository.existsByCrm("12345-SP")).thenReturn(true);

        // Act & Assert (Ação e Verificação)
        var exception = assertThrows(RuntimeException.class, () -> professionalService.create(dto));
        assertEquals("Erro: Este CRM já está cadastrado no sistema.", exception.getMessage());
        
        // Garante que o método save NUNCA foi chamado
        verify(professionalRepository, never()).save(any());
    }
}