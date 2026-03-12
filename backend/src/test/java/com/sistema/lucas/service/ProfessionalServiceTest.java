package com.sistema.lucas.service;

import com.sistema.lucas.model.Professional;
import com.sistema.lucas.model.ProfessionalCreateDTO;
import com.sistema.lucas.repository.ProfessionalRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder; // Adicionado import

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfessionalServiceTest {

    @InjectMocks
    private ProfessionalService professionalService;

    @Mock
    private ProfessionalRepository professionalRepository;

    @Mock
    private PasswordEncoder passwordEncoder; // ADICIONADO: O Service agora precisa do Encoder!

    @Test
    @DisplayName("Não deve cadastrar profissional com CRM duplicado")
    void cadastrarCrmDuplicado() {
        // Arrange
        var dto = new ProfessionalCreateDTO("Dr. House", "house@med.com", "senha123", "12345-SP", "Infectologia");
        when(professionalRepository.existsByCrm("12345-SP")).thenReturn(true);

        // Act & Assert
        var exception = assertThrows(RuntimeException.class, () -> professionalService.create(dto));
        assertEquals("Erro: Este CRM já está cadastrado no sistema.", exception.getMessage());
        
        verify(professionalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve cadastrar um profissional com sucesso quando os dados são válidos")
    void cadastrarComSucesso() {
        // Arrange
        var dto = new ProfessionalCreateDTO("Dr. House", "house@med.com", "senha123", "12345-SP", "Infectologia");
        
        when(professionalRepository.existsByCrm(dto.crm())).thenReturn(false);
        when(passwordEncoder.encode(dto.password())).thenReturn("senhaCriptografada");

        // Act
        assertDoesNotThrow(() -> professionalService.create(dto));

        // Assert
        verify(professionalRepository, times(1)).save(any(Professional.class));
    }
}