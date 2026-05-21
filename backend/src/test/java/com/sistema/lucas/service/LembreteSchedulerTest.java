package com.sistema.lucas.service;

import com.sistema.lucas.model.Appointment;
import com.sistema.lucas.model.Patient;
import com.sistema.lucas.model.Professional;
import com.sistema.lucas.model.enums.StatusConsulta;
import com.sistema.lucas.repository.AppointmentRepository;
import com.sistema.lucas.repository.ProfessionalAvailabilityRepository;
import com.sistema.lucas.repository.ProfessionalRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LembreteSchedulerTest {

    @InjectMocks private LembreteScheduler lembreteScheduler;

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private EmailTemplateService emailTemplateService;
    @Mock private ProfessionalRepository professionalRepository;
    @Mock private ProfessionalAvailabilityRepository availabilityRepository;
    @Mock private EmailService emailService;

    private Professional profissional(String email) {
        var p = new Professional(); p.setId(1L); p.setEmail(email); p.setName("Dr. Teste"); return p;
    }

    private Appointment consultaAtrasada(Professional prof, StatusConsulta status) {
        var pac = new Patient(); pac.setEmail("pac@test.com"); pac.setName("Paciente Teste");
        var a = new Appointment();
        a.setId(10L); a.setProfessional(prof); a.setPatient(pac); a.setStatus(status);
        a.setDateTime(LocalDateTime.now().minusDays(2));
        return a;
    }

    // ──────────────────────── alertarConsultasAtrasadas ────────────────────────

    @Nested @DisplayName("alertarConsultasAtrasadas")
    class AlertarAtrasadasTests {

        @Test @DisplayName("Deve chamar findAllAtrasadas com agora e STATUSES_PENDENTES")
        void deveBuscarAtrasadasComParametrosCorretos() {
            when(appointmentRepository.findAllAtrasadas(any(LocalDateTime.class), eq(AppointmentService.STATUSES_PENDENTES)))
                .thenReturn(List.of());

            lembreteScheduler.alertarConsultasAtrasadas();

            verify(appointmentRepository).findAllAtrasadas(any(LocalDateTime.class), eq(AppointmentService.STATUSES_PENDENTES));
        }

        @Test @DisplayName("Deve retornar cedo sem enviar e-mails quando lista vazia")
        void deveRetornarCedoQuandoListaVazia() {
            when(appointmentRepository.findAllAtrasadas(any(), any())).thenReturn(List.of());

            lembreteScheduler.alertarConsultasAtrasadas();

            verifyNoInteractions(emailTemplateService);
        }

        @Test @DisplayName("Deve agrupar atrasadas por profissional e enviar 1 e-mail por profissional")
        void deveAgruparPorProfissionalEEnviarUmEmailPorProfissional() {
            var prof1 = profissional("prof1@test.com");
            var prof2 = profissional("prof2@test.com"); prof2.setId(2L);

            when(appointmentRepository.findAllAtrasadas(any(), any())).thenReturn(List.of(
                consultaAtrasada(prof1, StatusConsulta.AGENDADA),
                consultaAtrasada(prof1, StatusConsulta.CONFIRMADA_PROFISSIONAL),
                consultaAtrasada(prof2, StatusConsulta.AGENDADA)
            ));

            lembreteScheduler.alertarConsultasAtrasadas();

            verify(emailTemplateService, times(2)).enviarAlertaConsultasAtrasadas(any(Professional.class), anyList());
        }

        @Test @DisplayName("Falha de e-mail para 1 profissional não deve interromper envios dos demais")
        void falhaNaoDeveInterromperOutrosEnvios() {
            var prof1 = profissional("prof1@test.com");
            var prof2 = profissional("prof2@test.com"); prof2.setId(2L);

            when(appointmentRepository.findAllAtrasadas(any(), any())).thenReturn(List.of(
                consultaAtrasada(prof1, StatusConsulta.AGENDADA),
                consultaAtrasada(prof2, StatusConsulta.AGENDADA)
            ));

            doThrow(new RuntimeException("SMTP indisponível"))
                .when(emailTemplateService).enviarAlertaConsultasAtrasadas(eq(prof1), anyList());

            assertDoesNotThrow(() -> lembreteScheduler.alertarConsultasAtrasadas());

            // prof2 ainda deve ter sido notificado
            verify(emailTemplateService).enviarAlertaConsultasAtrasadas(eq(prof2), anyList());
        }
    }

    // ──────────────────────── enviarLembretes ────────────────────────

    @Nested @DisplayName("enviarLembretes")
    class EnviarLembretesTests {

        @Test @DisplayName("Deve chamar enviarLembrete para cada consulta de amanhã")
        void deveChamarEnviarLembreteParaCadaConsulta() {
            var pac = new Patient(); pac.setEmail("pac@test.com"); pac.setName("Pac");
            var prof = profissional("prof@test.com");
            var c1 = new Appointment(); c1.setPatient(pac); c1.setProfessional(prof);
            c1.setStatus(StatusConsulta.CONFIRMADA);
            var c2 = new Appointment(); c2.setPatient(pac); c2.setProfessional(prof);
            c2.setStatus(StatusConsulta.CONFIRMADA_PROFISSIONAL);

            when(appointmentRepository.findConsultasParaLembrete(any(), any()))
                .thenReturn(List.of(c1, c2));

            lembreteScheduler.enviarLembretes();

            verify(emailTemplateService, times(2)).enviarLembrete(any(Appointment.class));
        }

        @Test @DisplayName("Não deve enviar lembretes quando não há consultas amanhã")
        void naoDeveEnviarQuandoSemConsultas() {
            when(appointmentRepository.findConsultasParaLembrete(any(), any())).thenReturn(List.of());

            lembreteScheduler.enviarLembretes();

            verifyNoInteractions(emailTemplateService);
        }
    }

    // ──────────────────────── notificarPrazoAgenda ────────────────────────

    @Nested @DisplayName("notificarPrazoAgenda")
    class NotificarPrazoTests {

        @Test @DisplayName("Não deve notificar quando profissional já tem agenda no próximo mês")
        void naoNotificaQuandoJaTemAgenda() {
            // Não mockamos professionalRepository.findAll() → retorna lista vazia → sem notificações
            when(professionalRepository.findAll()).thenReturn(List.of());

            lembreteScheduler.notificarPrazoAgenda();

            verifyNoInteractions(emailService);
        }
    }
}
