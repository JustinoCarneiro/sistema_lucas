package com.sistema.lucas.service;

import com.sistema.lucas.model.Patient;
import com.sistema.lucas.model.dto.PatientCreateDTO;
import com.sistema.lucas.repository.PatientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @InjectMocks
    private PatientService patientService;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Não deve cadastrar paciente com CPF já existente")
    void cadastrarCpfDuplicado() {
        // Arrange
        var dto = new PatientCreateDTO("Lucas Paciente", "paciente@teste.com", "123456", "111.222.333-44", "5511999998888", "Plano Saude X");
        when(patientRepository.existsByCpf(dto.cpf())).thenReturn(true);

        // Act & Assert
        var exception = assertThrows(RuntimeException.class, () -> patientService.create(dto));
        assertTrue(exception.getMessage().contains("CPF já cadastrado"));
        
        verify(patientRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve cadastrar um paciente com sucesso quando os dados são válidos")
    void cadastrarComSucesso() {
        // Arrange
        var dto = new PatientCreateDTO("Novo Paciente", "novo@teste.com", "senha123", "000.000.000-00", "5511000000000", "Plano Y");
        
        when(patientRepository.existsByCpf(dto.cpf())).thenReturn(false);
        // ADICIONADO: Simular que o e-mail também não existe no banco
        when(patientRepository.existsByEmail(dto.email())).thenReturn(false); 
        
        when(passwordEncoder.encode(dto.password())).thenReturn("senhaCriptografada");

        // Act
        assertDoesNotThrow(() -> patientService.create(dto));

        // Assert
        verify(patientRepository, times(1)).save(any(Patient.class));
    }
}