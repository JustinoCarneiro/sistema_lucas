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
@SuppressWarnings("null") // matchers Mockito (any()) retornam null por design
class AppointmentServiceTest {

    @InjectMocks
    private AppointmentService appointmentService;

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private ProfessionalRepository professionalRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailTemplateService emailTemplateService;
    @Mock private ProfessionalAvailabilityRepository availabilityRepository;
    @Mock private AuditLogService auditLogService;

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
            when(availabilityRepository.findByProfessionalEmailAndDate("ana@clinica.com", futureMonday.toLocalDate()))
                .thenReturn(List.of(availability));
            when(appointmentRepository.findByProfessionalIdAndDateTimeBetweenAndStatusNot(
                eq(professionalId), any(), any(), any()
            )).thenReturn(List.of());

            assertDoesNotThrow(() -> appointmentService.agendar(dto, patientEmail));

            var captor = org.mockito.ArgumentCaptor.forClass(Appointment.class);
            verify(appointmentRepository, times(1)).save(captor.capture());
            assertEquals(StatusConsulta.AGUARDANDO_CONFIRMACAO, captor.getValue().getStatus());
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
            when(availabilityRepository.findByProfessionalEmailAndDate("ana@clinica.com", futureMonday.toLocalDate()))
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
            when(availabilityRepository.findByProfessionalEmailAndDate("ana@clinica.com", futureMonday.toLocalDate()))
                .thenReturn(List.of(availability));
            when(appointmentRepository.findByProfessionalIdAndDateTimeBetweenAndStatusNot(
                eq(professionalId), any(), any(), any()
            )).thenReturn(List.of(existente));

            var exception = assertThrows(RuntimeException.class, () -> appointmentService.agendar(dto, patientEmail));
            assertTrue(exception.getMessage().contains("já está ocupado"));
        }
    }

    // ──────────────────────── Fluxo de Aprovação ────────────────────────

    @Nested
    @DisplayName("Aprovação e Recusa de Solicitações Pendentes")
    class AprovacaoTests {

        @Test
        @DisplayName("Profissional deve aprovar solicitação AGUARDANDO_CONFIRMACAO → AGENDADA")
        void aprovarAgendamento_sucesso() {
            Long id = 1L;
            String emailProf = "ana@clinica.com";

            var professional = new Professional();
            professional.setEmail(emailProf);

            var patient = new Patient();
            patient.setName("João da Silva");

            var consulta = new Appointment();
            consulta.setId(id);
            consulta.setProfessional(professional);
            consulta.setPatient(patient);
            consulta.setStatus(StatusConsulta.AGUARDANDO_CONFIRMACAO);
            consulta.setDateTime(LocalDateTime.now().plusDays(3));

            when(appointmentRepository.findById(id)).thenReturn(Optional.of(consulta));

            assertDoesNotThrow(() -> appointmentService.aprovarAgendamento(id, emailProf));
            assertEquals(StatusConsulta.AGENDADA, consulta.getStatus());
            verify(appointmentRepository, times(1)).save(consulta);
            verify(emailTemplateService, times(1)).notificarPacienteAgendamentoAceito(consulta);
        }

        @Test
        @DisplayName("Profissional deve recusar solicitação com justificativa → CANCELADA")
        void recusarAgendamento_comJustificativa() {
            Long id = 2L;
            String emailProf = "ana@clinica.com";
            String motivo = "Agenda lotada neste período.";

            var professional = new Professional();
            professional.setEmail(emailProf);

            var patient = new Patient();
            patient.setName("Maria Souza");

            var consulta = new Appointment();
            consulta.setId(id);
            consulta.setProfessional(professional);
            consulta.setPatient(patient);
            consulta.setStatus(StatusConsulta.AGUARDANDO_CONFIRMACAO);
            consulta.setDateTime(LocalDateTime.now().plusDays(2));

            when(appointmentRepository.findById(id)).thenReturn(Optional.of(consulta));

            assertDoesNotThrow(() -> appointmentService.recusarAgendamento(id, emailProf, motivo));
            assertEquals(StatusConsulta.CANCELADA, consulta.getStatus());
            assertEquals(motivo, consulta.getCancelReason());
            verify(emailTemplateService, times(1)).notificarPacienteAgendamentoRecusado(consulta, motivo);
        }

        @Test
        @DisplayName("Profissional errado não pode aprovar consulta de outro profissional")
        void aprovarAgendamento_profissionalErrado_lancaExcecao() {
            Long id = 3L;

            var professional = new Professional();
            professional.setEmail("dono@clinica.com");

            var consulta = new Appointment();
            consulta.setId(id);
            consulta.setProfessional(professional);
            consulta.setStatus(StatusConsulta.AGUARDANDO_CONFIRMACAO);

            when(appointmentRepository.findById(id)).thenReturn(Optional.of(consulta));

            var ex = assertThrows(RuntimeException.class,
                () -> appointmentService.aprovarAgendamento(id, "outro@clinica.com"));
            assertTrue(ex.getMessage().contains("não autorizada"));
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

    // ──────────────────────── Cancelamento e Reagendamento ────────────────────────

    @Nested
    @DisplayName("Cancelamento e Reagendamento (Regra 24h e Justificativa)")
    class CancelamentoReagendamentoTests {

        @Test
        @DisplayName("Deve cancelar com sucesso se faltar mais de 24h sem aplicar penalidade")
        void cancelarComSucesso() {
            Long id = 1L;
            String email = "paciente@email.com";
            String justificativa = "Imprevisto pessoal importante";

            var paciente = new Patient();
            paciente.setEmail(email);

            var consulta = new Appointment();
            consulta.setId(id);
            consulta.setPatient(paciente);
            consulta.setDateTime(LocalDateTime.now().plusHours(25)); // 25h > 24h — sem penalidade
            consulta.setStatus(StatusConsulta.AGENDADA);

            when(appointmentRepository.findById(id)).thenReturn(Optional.of(consulta));

            assertDoesNotThrow(() -> appointmentService.cancelar(id, email, justificativa));
            assertEquals(StatusConsulta.CANCELADA, consulta.getStatus());
            assertEquals(justificativa, consulta.getCancelReason());
            verify(auditLogService).log(anyString(), anyString(), anyString(), anyLong(), anyString());
            // Sem penalidade: nenhum e-mail de infração deve ter sido enviado
            verify(emailTemplateService, never()).enviarAvisoPrimeiraFalta(any(), any());
            verify(emailTemplateService, never()).enviarAvisoBloqueioFalta(any(), any(), any());
        }

        @Test
        @DisplayName("Cancelamento tardio (<24h) na 1ª infração aplica advertência, não bloqueia")
        void cancelarTardio_aplicaAdvertencia() {
            Long id = 1L;
            String email = "paciente@email.com";

            var paciente = new Patient(); // infractionCount=0, receivedFirstWarning=false por padrão
            paciente.setEmail(email);

            var consulta = new Appointment();
            consulta.setId(id);
            consulta.setPatient(paciente);
            consulta.setDateTime(LocalDateTime.now().plusHours(23)); // 23h < 24h → penalidade
            consulta.setStatus(StatusConsulta.AGENDADA);

            when(appointmentRepository.findById(id)).thenReturn(Optional.of(consulta));

            assertDoesNotThrow(() -> appointmentService.cancelar(id, email, "Emergência familiar"));

            assertEquals(StatusConsulta.CANCELADA, consulta.getStatus());
            assertEquals(1, paciente.getInfractionCount());
            assertTrue(paciente.isReceivedFirstWarning());
            assertNull(paciente.getBlockedUntil()); // 1ª infração: NÃO bloqueia
            verify(emailTemplateService, times(1)).enviarAvisoPrimeiraFalta(paciente, consulta);
            verify(emailTemplateService, never()).enviarAvisoBloqueioFalta(any(), any(), any());
            verify(patientRepository, times(1)).save(paciente);
        }

        @Test
        @DisplayName("Cancelamento tardio na 2ª infração bloqueia o paciente por 15 dias")
        void cancelarTardio_reincidencia_bloqueiaPaciente() {
            Long id = 2L;
            String email = "paciente@email.com";

            var paciente = new Patient();
            paciente.setEmail(email);
            paciente.setReceivedFirstWarning(true); // já foi advertido
            paciente.setInfractionCount(1);

            var consulta = new Appointment();
            consulta.setId(id);
            consulta.setPatient(paciente);
            consulta.setDateTime(LocalDateTime.now().plusHours(10)); // < 24h
            consulta.setStatus(StatusConsulta.CONFIRMADA);

            when(appointmentRepository.findById(id)).thenReturn(Optional.of(consulta));

            assertDoesNotThrow(() -> appointmentService.cancelar(id, email, "Não posso ir"));

            assertEquals(2, paciente.getInfractionCount());
            assertNotNull(paciente.getBlockedUntil());
            assertTrue(paciente.getBlockedUntil().isAfter(LocalDateTime.now().plusDays(14)));
            verify(emailTemplateService, times(1)).enviarAvisoBloqueioFalta(eq(paciente), eq(consulta), any());
            verify(emailTemplateService, never()).enviarAvisoPrimeiraFalta(any(), any());
        }

        @Test
        @DisplayName("Cancelamento de consulta AGUARDANDO_CONFIRMACAO não aplica penalidade")
        void cancelarConsultaPendente_semPenalidade() {
            Long id = 3L;
            String email = "paciente@email.com";

            var paciente = new Patient();
            paciente.setEmail(email);

            var consulta = new Appointment();
            consulta.setId(id);
            consulta.setPatient(paciente);
            consulta.setDateTime(LocalDateTime.now().plusHours(2)); // < 24h mas pendente
            consulta.setStatus(StatusConsulta.AGUARDANDO_CONFIRMACAO);

            when(appointmentRepository.findById(id)).thenReturn(Optional.of(consulta));

            assertDoesNotThrow(() -> appointmentService.cancelar(id, email, "Desisti do agendamento"));

            assertEquals(StatusConsulta.CANCELADA, consulta.getStatus());
            verify(emailTemplateService, never()).enviarAvisoPrimeiraFalta(any(), any());
            verify(emailTemplateService, never()).enviarAvisoBloqueioFalta(any(), any(), any());
            verify(patientRepository, never()).save(any()); // nenhum save de penalidade
        }

        @Test
        @DisplayName("Deve falhar ao reagendar sem justificativa")
        void erroReagendarSemJustificativa() {
            Long id = 1L;
            String email = "paciente@email.com";
            LocalDateTime novaData = LocalDateTime.now().plusDays(2);

            var paciente = new Patient();
            paciente.setEmail(email);

            var consulta = new Appointment();
            consulta.setId(id);
            consulta.setPatient(paciente);
            consulta.setDateTime(LocalDateTime.now().plusHours(25)); // > 24h: sem penalidade antes do throw

            when(appointmentRepository.findById(id)).thenReturn(Optional.of(consulta));

            var ex = assertThrows(RuntimeException.class, () -> appointmentService.reagendar(id, email, novaData, ""));
            assertEquals("A justificativa é obrigatória para o reagendamento.", ex.getMessage());
        }
    }

    // ──────────────────────── Falta ────────────────────────

    @Nested
    @DisplayName("Marcação de Falta (Sistema de Infrações Progressivas)")
    class FaltaTests {

        @Test
        @DisplayName("1ª falta: envia advertência por e-mail e não bloqueia o paciente")
        void primeiraFalta_deveEnviarAdvertencia() {
            Long id = 10L;
            String emailProf = "ana@clinica.com";

            var professional = new Professional();
            professional.setEmail(emailProf);

            var patient = new Patient(); // infractionCount=0, receivedFirstWarning=false
            patient.setEmail("lucas@email.com");

            var consulta = new Appointment();
            consulta.setId(id);
            consulta.setProfessional(professional);
            consulta.setPatient(patient);
            consulta.setDateTime(LocalDateTime.now().minusHours(1));
            consulta.setStatus(StatusConsulta.CONFIRMADA);

            when(appointmentRepository.findById(id)).thenReturn(Optional.of(consulta));

            assertDoesNotThrow(() -> appointmentService.marcarFalta(id, emailProf));

            assertEquals(StatusConsulta.FALTA, consulta.getStatus());
            assertEquals(1, patient.getInfractionCount());
            assertTrue(patient.isReceivedFirstWarning());
            assertNull(patient.getBlockedUntil()); // NÃO bloqueado
            verify(emailTemplateService, times(1)).enviarAvisoPrimeiraFalta(patient, consulta);
            verify(emailTemplateService, never()).enviarAvisoBloqueioFalta(any(), any(), any());
        }

        @Test
        @DisplayName("2ª falta: bloqueia o paciente por 15 dias e envia e-mail de bloqueio")
        void segundaFalta_deveBloquearPaciente() {
            Long id = 11L;
            String emailProf = "ana@clinica.com";

            var professional = new Professional();
            professional.setEmail(emailProf);

            var patient = new Patient();
            patient.setEmail("lucas@email.com");
            patient.setReceivedFirstWarning(true); // já advertido
            patient.setInfractionCount(1);

            var consulta = new Appointment();
            consulta.setId(id);
            consulta.setProfessional(professional);
            consulta.setPatient(patient);
            consulta.setDateTime(LocalDateTime.now().minusHours(2));
            consulta.setStatus(StatusConsulta.CONFIRMADA);

            when(appointmentRepository.findById(id)).thenReturn(Optional.of(consulta));

            assertDoesNotThrow(() -> appointmentService.marcarFalta(id, emailProf));

            assertEquals(StatusConsulta.FALTA, consulta.getStatus());
            assertEquals(2, patient.getInfractionCount());
            assertNotNull(patient.getBlockedUntil());
            assertTrue(patient.getBlockedUntil().isAfter(LocalDateTime.now().plusDays(14)));
            verify(emailTemplateService, times(1)).enviarAvisoBloqueioFalta(eq(patient), eq(consulta), any());
            verify(emailTemplateService, never()).enviarAvisoPrimeiraFalta(any(), any());
        }

        @Test
        @DisplayName("Profissional errado não pode marcar falta em consulta de outro")
        void falta_profissionalErrado_lancaExcecao() {
            Long id = 12L;

            var professional = new Professional();
            professional.setEmail("dono@clinica.com");

            var consulta = new Appointment();
            consulta.setId(id);
            consulta.setProfessional(professional);

            when(appointmentRepository.findById(id)).thenReturn(Optional.of(consulta));

            var ex = assertThrows(RuntimeException.class,
                () -> appointmentService.marcarFalta(id, "outro@clinica.com"));
            assertTrue(ex.getMessage().contains("não é o médico"));
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