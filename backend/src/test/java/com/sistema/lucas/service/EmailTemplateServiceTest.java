package com.sistema.lucas.service;

import com.sistema.lucas.model.Appointment;
import com.sistema.lucas.model.Patient;
import com.sistema.lucas.model.Professional;
import com.sistema.lucas.model.enums.StatusConsulta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailTemplateServiceTest {

    @InjectMocks private EmailTemplateService emailTemplateService;
    @Mock private EmailService emailService;

    // ──────────────────────── Alerta de Atrasadas ────────────────────────

    @Nested @DisplayName("enviarAlertaConsultasAtrasadas")
    class AlertaAtrasadasTests {

        private Professional profissional() {
            var p = new Professional();
            p.setEmail("prof@clinica.com"); p.setName("Dra. Ana");
            return p;
        }

        private Appointment consultaAtrasada(String pacienteNome, StatusConsulta status) {
            var pac = new Patient(); pac.setName(pacienteNome);
            var a = new Appointment();
            a.setPatient(pac); a.setStatus(status);
            a.setDateTime(LocalDateTime.of(2026, 5, 1, 9, 0));
            return a;
        }

        @Test @DisplayName("Deve enviar e-mail com assunto contendo URGENTE")
        void deveChamarEmailComAssuntoUrgente() {
            var atrasadas = List.of(consultaAtrasada("Lucas", StatusConsulta.AGENDADA));

            emailTemplateService.enviarAlertaConsultasAtrasadas(profissional(), atrasadas);

            var captor = ArgumentCaptor.forClass(String.class);
            verify(emailService).enviar(eq("prof@clinica.com"), captor.capture(), anyString());
            assertTrue(captor.getValue().contains("URGENTE"));
        }

        @Test @DisplayName("Corpo do e-mail deve conter nome de cada paciente atrasado")
        void deveConterNomeDoPacienteNoCorpo() {
            var atrasadas = List.of(
                consultaAtrasada("Lucas Silva", StatusConsulta.AGENDADA),
                consultaAtrasada("Maria Souza", StatusConsulta.CONFIRMADA_PROFISSIONAL)
            );

            emailTemplateService.enviarAlertaConsultasAtrasadas(profissional(), atrasadas);

            var captor = ArgumentCaptor.forClass(String.class);
            verify(emailService).enviar(anyString(), anyString(), captor.capture());
            String corpo = captor.getValue();
            assertTrue(corpo.contains("Lucas Silva"));
            assertTrue(corpo.contains("Maria Souza"));
        }

        @Test @DisplayName("Corpo deve conter status de cada consulta atrasada")
        void deveConterStatusDaConsultaNoCorpo() {
            var atrasadas = List.of(consultaAtrasada("Lucas", StatusConsulta.AGENDADA));

            emailTemplateService.enviarAlertaConsultasAtrasadas(profissional(), atrasadas);

            var captor = ArgumentCaptor.forClass(String.class);
            verify(emailService).enviar(anyString(), anyString(), captor.capture());
            assertTrue(captor.getValue().contains("AGENDADA"));
        }

        @Test @DisplayName("Corpo deve conter link para a aba de consultas do profissional")
        void deveConterLinkParaAgendaProfissional() {
            var atrasadas = List.of(consultaAtrasada("Lucas", StatusConsulta.AGENDADA));

            emailTemplateService.enviarAlertaConsultasAtrasadas(profissional(), atrasadas);

            var captor = ArgumentCaptor.forClass(String.class);
            verify(emailService).enviar(anyString(), anyString(), captor.capture());
            assertTrue(captor.getValue().contains("professional-appointments"));
        }

        @Test @DisplayName("Deve enviar para o e-mail do profissional correto")
        void deveEnviarParaEmailCorreto() {
            var atrasadas = List.of(consultaAtrasada("Lucas", StatusConsulta.AGENDADA));

            emailTemplateService.enviarAlertaConsultasAtrasadas(profissional(), atrasadas);

            verify(emailService).enviar(eq("prof@clinica.com"), anyString(), anyString());
        }
    }

    // ──────────────────────── Lembrete ────────────────────────

    @Nested @DisplayName("enviarLembrete")
    class LembreteTests {

        @Test @DisplayName("Deve enviar lembrete para o e-mail do paciente")
        void deveEnviarParaEmailDoPaciente() {
            var pac = new Patient(); pac.setEmail("lucas@email.com"); pac.setName("Lucas");
            var prof = new Professional(); prof.setName("Dra. Ana"); prof.setSpecialty("Psicologia");
            var consulta = new Appointment();
            consulta.setPatient(pac); consulta.setProfessional(prof);
            consulta.setDateTime(LocalDateTime.now().plusDays(1));

            emailTemplateService.enviarLembrete(consulta);

            verify(emailService).enviar(eq("lucas@email.com"), anyString(), anyString());
        }
    }
}
