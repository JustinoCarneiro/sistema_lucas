package com.sistema.lucas.service;

import com.sistema.lucas.model.Appointment;
import com.sistema.lucas.model.Patient;
import com.sistema.lucas.model.Professional;
import com.sistema.lucas.model.dto.AppointmentCreateDTO;
import com.sistema.lucas.repository.AppointmentRepository;
import com.sistema.lucas.repository.PatientRepository;
import com.sistema.lucas.repository.ProfessionalRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @InjectMocks
    private AppointmentService appointmentService;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ProfessionalRepository professionalRepository;

    @Mock
    private PatientRepository patientRepository;

    @Test
    @DisplayName("Deve agendar uma consulta com sucesso")
    void agendarComSucesso() {
        // Arrange
        Long professionalId = 1L;
        String patientEmail = "lucas@email.com";
        // DTO atualizado: removido o patientId conforme o novo contrato
        var dto = new AppointmentCreateDTO(professionalId, LocalDateTime.now().plusDays(1), "Consulta de rotina");

        var professional = new Professional();
        professional.setId(professionalId);

        var patient = new Patient();
        patient.setEmail(patientEmail);

        when(professionalRepository.findById(professionalId)).thenReturn(Optional.of(professional));
        // Mock alterado: Agora o service busca por e-mail (Token)
        when(patientRepository.findByEmail(patientEmail)).thenReturn(Optional.of(patient));

        // Act - Chamada atualizada com o parâmetro de e-mail
        assertDoesNotThrow(() -> appointmentService.schedule(dto, patientEmail));

        // Assert
        verify(appointmentRepository, times(1)).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Deve lançar erro ao agendar com profissional inexistente")
    void erroProfissionalInexistente() {
        // Arrange
        String patientEmail = "lucas@email.com";
        var dto = new AppointmentCreateDTO(99L, LocalDateTime.now().plusDays(1), "Razão");
        when(professionalRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        var exception = assertThrows(RuntimeException.class, () -> appointmentService.schedule(dto, patientEmail));
        assertTrue(exception.getMessage().contains("Profissional não encontrado"));
        
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar erro ao agendar com paciente inexistente")
    void erroPacienteInexistente() {
        // Arrange
        Long profId = 1L;
        String patientEmail = "inexistente@email.com";
        var dto = new AppointmentCreateDTO(profId, LocalDateTime.now().plusDays(1), "Razão");
        
        when(professionalRepository.findById(profId)).thenReturn(Optional.of(new Professional()));
        // Mock alterado para findByEmail
        when(patientRepository.findByEmail(patientEmail)).thenReturn(Optional.empty());

        // Act & Assert
        var exception = assertThrows(RuntimeException.class, () -> appointmentService.schedule(dto, patientEmail));
        assertTrue(exception.getMessage().contains("Paciente não encontrado"));
        
        verify(appointmentRepository, never()).save(any());
    }
}