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
        Long patientId = 1L;
        var dto = new AppointmentCreateDTO(professionalId, patientId, LocalDateTime.now().plusDays(1), "Consulta de rotina");

        var professional = new Professional();
        professional.setId(professionalId);

        var patient = new Patient();
        patient.setId(patientId);

        when(professionalRepository.findById(professionalId)).thenReturn(Optional.of(professional));
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));

        // Act
        assertDoesNotThrow(() -> appointmentService.schedule(dto));

        // Assert
        verify(appointmentRepository, times(1)).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Deve lançar erro ao agendar com profissional inexistente")
    void erroProfissionalInexistente() {
        // Arrange
        var dto = new AppointmentCreateDTO(99L, 1L, LocalDateTime.now().plusDays(1), "Razão");
        when(professionalRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        var exception = assertThrows(RuntimeException.class, () -> appointmentService.schedule(dto));
        assertTrue(exception.getMessage().contains("Profissional não encontrado"));
        
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar erro ao agendar com paciente inexistente")
    void erroPacienteInexistente() {
        // Arrange
        Long profId = 1L;
        var dto = new AppointmentCreateDTO(profId, 88L, LocalDateTime.now().plusDays(1), "Razão");
        
        when(professionalRepository.findById(profId)).thenReturn(Optional.of(new Professional()));
        when(patientRepository.findById(88L)).thenReturn(Optional.empty());

        // Act & Assert
        var exception = assertThrows(RuntimeException.class, () -> appointmentService.schedule(dto));
        assertTrue(exception.getMessage().contains("Paciente não encontrado"));
        
        verify(appointmentRepository, never()).save(any());
    }
}