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
        @DisplayName("Deve agendar uma consulta com sucesso quando slot está disponível")
        void agendarComSucesso() {
            Long professionalId = 1L;
            String patientEmail = "lucas@email.com";
            // Pega uma data futura que caia em segunda-feira
            LocalDateTime futureMonday = getNextDayOfWeek(DayOfWeek.MONDAY).atTime(9, 0);
            var dto = new AppointmentCreateDTO(professionalId, futureMonday, "Consulta de rotina");

            var professional = new Professional();
            professional.setId(professionalId);
            professional.setEmail("ana@clinica.com");

            var patient = new Patient();
            patient.setEmail(patientEmail);

            var availability = new ProfessionalAvailability();
            availability.setStartTime(LocalTime.of(8, 0));
            availability.setEndTime(LocalTime.of(12, 0));

            when(professionalRepository.findById(professionalId)).thenReturn(Optional.of(professional));
            when(patientRepository.findByEmail(patientEmail)).thenReturn(Optional.of(patient));
            when(availabilityRepository.findByProfessionalEmailAndDayOfWeek("ana@clinica.com", DayOfWeek.MONDAY))
                .thenReturn(Optional.of(availability));
            when(appointmentRepository.findByProfessionalIdAndDateTimeBetweenAndStatusNot(
                eq(professionalId), any(), any(), any()
            )).thenReturn(List.of());

            assertDoesNotThrow(() -> appointmentService.agendar(dto, patientEmail));
            verify(appointmentRepository, times(1)).save(any(Appointment.class));
            verify(emailTemplateService, times(1)).notificarConsultaAgendada(any());
        }

        @Test
        @DisplayName("Deve lançar erro ao agendar com profissional inexistente")
        void erroProfissionalInexistente() {
            String patientEmail = "lucas@email.com";
            var dto = new AppointmentCreateDTO(99L, LocalDateTime.now().plusDays(1), "Razão");
            when(professionalRepository.findById(99L)).thenReturn(Optional.empty());

            var exception = assertThrows(RuntimeException.class, () -> appointmentService.agendar(dto, patientEmail));
            assertTrue(exception.getMessage().contains("Profissional não encontrado"));
            verify(appointmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar erro ao agendar com paciente inexistente")
        void erroPacienteInexistente() {
            Long profId = 1L;
            String patientEmail = "inexistente@email.com";
            var dto = new AppointmentCreateDTO(profId, LocalDateTime.now().plusDays(1), "Razão");

            when(professionalRepository.findById(profId)).thenReturn(Optional.of(new Professional()));
            when(patientRepository.findByEmail(patientEmail)).thenReturn(Optional.empty());

            var exception = assertThrows(RuntimeException.class, () -> appointmentService.agendar(dto, patientEmail));
            assertTrue(exception.getMessage().contains("Paciente não encontrado"));
            verify(appointmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar erro ao agendar em dia sem disponibilidade")
        void erroSemDisponibilidade() {
            Long professionalId = 1L;
            String patientEmail = "lucas@email.com";
            LocalDateTime futureMonday = getNextDayOfWeek(DayOfWeek.MONDAY).atTime(9, 0);
            var dto = new AppointmentCreateDTO(professionalId, futureMonday, "Consulta");

            var professional = new Professional();
            professional.setId(professionalId);
            professional.setEmail("ana@clinica.com");

            when(professionalRepository.findById(professionalId)).thenReturn(Optional.of(professional));
            when(patientRepository.findByEmail(patientEmail)).thenReturn(Optional.of(new Patient()));
            when(availabilityRepository.findByProfessionalEmailAndDayOfWeek("ana@clinica.com", DayOfWeek.MONDAY))
                .thenReturn(Optional.empty());

            var exception = assertThrows(RuntimeException.class, () -> appointmentService.agendar(dto, patientEmail));
            assertTrue(exception.getMessage().contains("não atende neste dia"));
            verify(appointmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar erro ao agendar fora da janela de atendimento")
        void erroForaDaJanela() {
            Long professionalId = 1L;
            String patientEmail = "lucas@email.com";
            // Agendar às 14:00, mas profissional atende das 08:00 às 12:00
            LocalDateTime futureMonday = getNextDayOfWeek(DayOfWeek.MONDAY).atTime(14, 0);
            var dto = new AppointmentCreateDTO(professionalId, futureMonday, "Consulta");

            var professional = new Professional();
            professional.setId(professionalId);
            professional.setEmail("ana@clinica.com");

            var availability = new ProfessionalAvailability();
            availability.setStartTime(LocalTime.of(8, 0));
            availability.setEndTime(LocalTime.of(12, 0));

            when(professionalRepository.findById(professionalId)).thenReturn(Optional.of(professional));
            when(patientRepository.findByEmail(patientEmail)).thenReturn(Optional.of(new Patient()));
            when(availabilityRepository.findByProfessionalEmailAndDayOfWeek("ana@clinica.com", DayOfWeek.MONDAY))
                .thenReturn(Optional.of(availability));

            var exception = assertThrows(RuntimeException.class, () -> appointmentService.agendar(dto, patientEmail));
            assertTrue(exception.getMessage().contains("fora da janela"));
            verify(appointmentRepository, never()).save(any());
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
            availability.setStartTime(LocalTime.of(8, 0));
            availability.setEndTime(LocalTime.of(12, 0));

            // Simula conflito: já existe consulta às 09:00
            var existente = new Appointment();
            existente.setDateTime(futureMonday);

            when(professionalRepository.findById(professionalId)).thenReturn(Optional.of(professional));
            when(patientRepository.findByEmail(patientEmail)).thenReturn(Optional.of(new Patient()));
            when(availabilityRepository.findByProfessionalEmailAndDayOfWeek("ana@clinica.com", DayOfWeek.MONDAY))
                .thenReturn(Optional.of(availability));
            when(appointmentRepository.findByProfessionalIdAndDateTimeBetweenAndStatusNot(
                eq(professionalId), any(), any(), any()
            )).thenReturn(List.of(existente));

            var exception = assertThrows(RuntimeException.class, () -> appointmentService.agendar(dto, patientEmail));
            assertTrue(exception.getMessage().contains("já está ocupado"));
            verify(appointmentRepository, never()).save(any());
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

            var profissional = new Professional();
            profissional.setEmail(emailProf);

            var consulta = new Appointment();
            consulta.setId(id);
            consulta.setProfessional(profissional);
            consulta.setStatus(StatusConsulta.AGENDADA);
            consulta.setDateTime(LocalDateTime.now().plusDays(3)); // > 24h

            when(appointmentRepository.findById(id)).thenReturn(Optional.of(consulta));

            assertDoesNotThrow(() -> appointmentService.confirmarProfissional(id, emailProf));
            assertEquals(StatusConsulta.CONFIRMADA_PROFISSIONAL, consulta.getStatus());
            verify(appointmentRepository, times(1)).save(consulta);
        }

        @Test
        @DisplayName("Profissional NÃO deve confirmar consulta já confirmada")
        void profissionalNaoConfirmaDuplicado() {
            Long id = 1L;
            String emailProf = "ana@clinica.com";

            var profissional = new Professional();
            profissional.setEmail(emailProf);

            var consulta = new Appointment();
            consulta.setId(id);
            consulta.setProfessional(profissional);
            consulta.setStatus(StatusConsulta.CONFIRMADA_PROFISSIONAL);
            consulta.setDateTime(LocalDateTime.now().plusDays(3));

            when(appointmentRepository.findById(id)).thenReturn(Optional.of(consulta));

            var exception = assertThrows(RuntimeException.class,
                () -> appointmentService.confirmarProfissional(id, emailProf));
            assertTrue(exception.getMessage().contains("já confirmada"));
        }

        @Test
        @DisplayName("Paciente deve confirmar após profissional → CONFIRMADA")
        void pacienteConfirmaAposProfissional() {
            Long id = 1L;
            String emailPac = "lucas@email.com";

            var paciente = new Patient();
            paciente.setEmail(emailPac);

            var consulta = new Appointment();
            consulta.setId(id);
            consulta.setPatient(paciente);
            consulta.setStatus(StatusConsulta.CONFIRMADA_PROFISSIONAL);
            consulta.setDateTime(LocalDateTime.now().plusDays(3));

            when(appointmentRepository.findById(id)).thenReturn(Optional.of(consulta));

            assertDoesNotThrow(() -> appointmentService.confirmarPaciente(id, emailPac));
            assertEquals(StatusConsulta.CONFIRMADA, consulta.getStatus());
            verify(emailTemplateService, times(1)).notificarConsultaConfirmada(consulta);
        }

        @Test
        @DisplayName("Paciente NÃO deve confirmar antes do profissional (status AGENDADA)")
        void pacienteNaoConfirmaAntesDoProfissional() {
            Long id = 1L;
            String emailPac = "lucas@email.com";

            var paciente = new Patient();
            paciente.setEmail(emailPac);

            var consulta = new Appointment();
            consulta.setId(id);
            consulta.setPatient(paciente);
            consulta.setStatus(StatusConsulta.AGENDADA);
            consulta.setDateTime(LocalDateTime.now().plusDays(3));

            when(appointmentRepository.findById(id)).thenReturn(Optional.of(consulta));

            var exception = assertThrows(RuntimeException.class,
                () -> appointmentService.confirmarPaciente(id, emailPac));
            assertTrue(exception.getMessage().contains("Aguardando confirmação do profissional"));
        }

        @Test
        @DisplayName("Confirmação deve falhar se faltam menos de 24h")
        void erroMenosDe24h() {
            Long id = 1L;
            String emailProf = "ana@clinica.com";

            var profissional = new Professional();
            profissional.setEmail(emailProf);

            var consulta = new Appointment();
            consulta.setId(id);
            consulta.setProfessional(profissional);
            consulta.setStatus(StatusConsulta.AGENDADA);
            consulta.setDateTime(LocalDateTime.now().plusHours(12)); // < 24h

            when(appointmentRepository.findById(id)).thenReturn(Optional.of(consulta));

            var exception = assertThrows(RuntimeException.class,
                () -> appointmentService.confirmarProfissional(id, emailProf));
            assertTrue(exception.getMessage().contains("menos de 24h"));
        }

        @Test
        @DisplayName("Profissional não deve confirmar consulta de outro profissional")
        void erroProfissionalErrado() {
            Long id = 1L;

            var profissional = new Professional();
            profissional.setEmail("outro@clinica.com");

            var consulta = new Appointment();
            consulta.setId(id);
            consulta.setProfessional(profissional);
            consulta.setStatus(StatusConsulta.AGENDADA);
            consulta.setDateTime(LocalDateTime.now().plusDays(3));

            when(appointmentRepository.findById(id)).thenReturn(Optional.of(consulta));

            var exception = assertThrows(RuntimeException.class,
                () -> appointmentService.confirmarProfissional(id, "ana@clinica.com"));
            assertTrue(exception.getMessage().contains("Sem permissão"));
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