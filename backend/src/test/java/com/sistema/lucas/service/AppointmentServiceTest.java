package com.sistema.lucas.service;

import com.sistema.lucas.model.Appointment;
import com.sistema.lucas.model.Patient;
import com.sistema.lucas.model.Professional;
import com.sistema.lucas.model.ProfessionalAvailability;
import com.sistema.lucas.model.dto.AppointmentCreateDTO;
import com.sistema.lucas.model.enums.StatusConsulta;
import com.sistema.lucas.repository.AppointmentRepository;
import com.sistema.lucas.repository.PatientRepository;
import com.sistema.lucas.repository.ProfessionalAvailabilityRepository;
import com.sistema.lucas.repository.ProfessionalRepository;
import com.sistema.lucas.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @InjectMocks
    private AppointmentService appointmentService;

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private ProfessionalRepository professionalRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailTemplateService emailTemplateService;
    @Mock private ProfessionalAvailabilityRepository availabilityRepository;

    // ──────────────────────── Agendamento ────────────────────────

    @Nested
    @DisplayName("Agendamento de Consultas")
    class AgendamentoTests {

        @Test
        @DisplayName("Deve agendar uma consulta com sucesso quando slot existe")
        void agendarComSucesso() {
            Long professionalId = 1L;
            String patientEmail = "lucas@email.com";
            LocalDateTime futureMonday = getNextDayOfWeek(DayOfWeek.MONDAY).atTime(9, 0);
            var dto = new AppointmentCreateDTO(professionalId, futureMonday, "Consulta de rotina");

            var professional = new Professional();
            professional.setId(professionalId);
            professional.setEmail("ana@clinica.com");

            var patient = new Patient();
            patient.setEmail(patientEmail);

            var availability = new ProfessionalAvailability();
            availability.setStartTime(LocalTime.of(9, 0));

            when(professionalRepository.findById(professionalId)).thenReturn(Optional.of(professional));
            when(patientRepository.findByEmail(patientEmail)).thenReturn(Optional.of(patient));
            when(availabilityRepository.findByProfessionalEmailAndDayOfWeek("ana@clinica.com", DayOfWeek.MONDAY))
                .thenReturn(List.of(availability));
            when(appointmentRepository.findByProfessionalIdAndDateTimeBetweenAndStatusNot(
                eq(professionalId), any(), any(), any()
            )).thenReturn(List.of());

            assertDoesNotThrow(() -> appointmentService.agendar(dto, patientEmail));
            verify(appointmentRepository, times(1)).save(any(Appointment.class));
        }

        @Test
        @DisplayName("Deve lançar erro ao agendar em dia/horário sem disponibilidade")
        void erroSemDisponibilidade() {
            Long professionalId = 1L;
            String patientEmail = "lucas@email.com";
            LocalDateTime futureMonday = getNextDayOfWeek(DayOfWeek.MONDAY).atTime(15, 0);
            var dto = new AppointmentCreateDTO(professionalId, futureMonday, "Consulta");

            var professional = new Professional();
            professional.setId(professionalId);
            professional.setEmail("ana@clinica.com");

            var slot = new ProfessionalAvailability();
            slot.setStartTime(LocalTime.of(8, 0));

            when(professionalRepository.findById(professionalId)).thenReturn(Optional.of(professional));
            when(patientRepository.findByEmail(patientEmail)).thenReturn(Optional.of(new Patient()));
            when(availabilityRepository.findByProfessionalEmailAndDayOfWeek("ana@clinica.com", DayOfWeek.MONDAY))
                .thenReturn(List.of(slot)); // Só tem 08:00, pedimos 15:00

            var exception = assertThrows(RuntimeException.class, () -> appointmentService.agendar(dto, patientEmail));
            assertEquals("O profissional não atende neste horário ou dia da semana.", exception.getMessage());
        }

        @Test
        @DisplayName("Deve lançar erro ao agendar em horário já ocupado")
        void erroHorarioOcupado() {
            Long professionalId = 1L;
            String patientEmail = "lucas@email.com";
            LocalDateTime futureMonday = getNextDayOfWeek(DayOfWeek.MONDAY).atTime(9, 0);
            var dto = new AppointmentCreateDTO(professionalId, futureMonday, "Consulta");

            var professional = new Professional();
            professional.setId(professionalId);
            professional.setEmail("ana@clinica.com");

            var availability = new ProfessionalAvailability();
            availability.setStartTime(LocalTime.of(9, 0));

            // Simula conflito: já existe consulta às 09:00
            var existente = new Appointment();
            existente.setDateTime(futureMonday);

            when(professionalRepository.findById(professionalId)).thenReturn(Optional.of(professional));
            when(patientRepository.findByEmail(patientEmail)).thenReturn(Optional.of(new Patient()));
            when(availabilityRepository.findByProfessionalEmailAndDayOfWeek("ana@clinica.com", DayOfWeek.MONDAY))
                .thenReturn(List.of(availability));
            when(appointmentRepository.findByProfessionalIdAndDateTimeBetweenAndStatusNot(
                eq(professionalId), any(), any(), any()
            )).thenReturn(List.of(existente));

            var exception = assertThrows(RuntimeException.class, () -> appointmentService.agendar(dto, patientEmail));
            assertTrue(exception.getMessage().contains("já está ocupado"));
        }
    }

    // ──────────────────────── Fluxo de Confirmação ────────────────────────

    @Nested
    @DisplayName("Fluxo de Confirmação (Profissional → Paciente)")
    class ConfirmacaoTests {

        @Test
        @DisplayName("Profissional deve confirmar consulta AGENDADA → CONFIRMADA_PROFISSIONAL")
        void profissionalConfirmaAgendada() {
            Long id = 1L;
            String emailProf = "ana@clinica.com";

            var professional = new Professional();
            professional.setEmail(emailProf);

            var consulta = new Appointment();
            consulta.setId(id);
            consulta.setProfessional(professional);
            consulta.setStatus(StatusConsulta.AGENDADA);
            consulta.setDateTime(LocalDateTime.now().plusDays(3)); // > 24h

            when(appointmentRepository.findById(id)).thenReturn(Optional.of(consulta));

            assertDoesNotThrow(() -> appointmentService.confirmarProfissional(id, emailProf));
            assertEquals(StatusConsulta.CONFIRMADA_PROFISSIONAL, consulta.getStatus());
            verify(appointmentRepository, times(1)).save(consulta);
        }
    }

    // ──────────────────────── Helper ────────────────────────

    private java.time.LocalDate getNextDayOfWeek(DayOfWeek target) {
        java.time.LocalDate date = java.time.LocalDate.now().plusDays(1);
        while (date.getDayOfWeek() != target) {
            date = date.plusDays(1);
        }
        return date;
    }
}