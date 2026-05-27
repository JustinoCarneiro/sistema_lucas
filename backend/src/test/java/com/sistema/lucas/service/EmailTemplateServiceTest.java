package com.sistema.lucas.service;

import com.sistema.lucas.model.Appointment;
import com.sistema.lucas.model.Patient;
import com.sistema.lucas.model.Professional;
import com.sistema.lucas.model.enums.StatusConsulta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailTemplateServiceTest {

    @InjectMocks private EmailTemplateService emailTemplateService;
    @Mock private EmailService emailService;

    @BeforeEach
    void configurarFrontendUrl() {
        // @Value não é injetado pelo Mockito; preenchemos manualmente para refletir produção.
        ReflectionTestUtils.setField(Objects.requireNonNull(emailTemplateService), "frontendUrl", "https://lucas.example.org");
    }

    // ──────────────── buildTemplate — placeholders vs argumentos ────────────────
    //
    // Regressão para o bug em produção (2026-05-27): buildTemplate quebrou com
    // `MissingFormatArgumentException: Format specifier '%s'` porque um text block
    // tinha 7 placeholders e .formatted() recebia 6 argumentos. Os testes abaixo
    // exercitam os fluxos públicos que passam por buildTemplate. Se a quantidade
    // de %s e argumentos voltar a divergir, qualquer um destes testes falha com
    // MissingFormatArgumentException antes mesmo do verify().

    @Nested @DisplayName("buildTemplate — fluxos que dispararam o erro em produção")
    class BuildTemplateTests {

        private Appointment consultaCompleta() {
            var pac = new Patient();
            pac.setEmail("paciente@email.com");
            pac.setName("João Paciente");

            var prof = new Professional();
            prof.setEmail("dra.ana@clinica.com");
            prof.setName("Dra. Ana Cláudia");

            var consulta = new Appointment();
            consulta.setPatient(pac);
            consulta.setProfessional(prof);
            consulta.setDateTime(LocalDateTime.of(2026, 6, 3, 16, 0));
            consulta.setReason("Consulta inicial");
            return consulta;
        }

        @Test @DisplayName("notificarPacienteAgendamentoPendente: não lança MissingFormatArgumentException (regressão produção)")
        void notificarPacienteAgendamentoPendente_naoLancaErroDeFormat() {
            assertDoesNotThrow(() ->
                emailTemplateService.notificarPacienteAgendamentoPendente(consultaCompleta())
            );

            var corpo = ArgumentCaptor.forClass(String.class);
            verify(emailService).enviar(eq("paciente@email.com"), anyString(), corpo.capture());

            String html = corpo.getValue();
            assertTrue(html.contains("Dra. Ana Cláudia"), "deveria conter o nome do profissional");
            assertTrue(html.contains("João Paciente"), "deveria conter o nome do paciente");
            assertTrue(html.contains("Acessar o Sistema"), "deveria conter o botão CTA");
            assertTrue(html.contains("https://lucas.example.org"), "href do CTA deveria apontar para frontendUrl");
            assertFalse(html.contains("%s"), "nenhum placeholder %s pode ter ficado para trás");
        }

        @Test @DisplayName("notificarConsultaAgendada: envia para paciente e profissional sem erro de format")
        void notificarConsultaAgendada_enviaParaAmbos() {
            assertDoesNotThrow(() ->
                emailTemplateService.notificarConsultaAgendada(consultaCompleta())
            );

            verify(emailService).enviar(eq("paciente@email.com"), anyString(), anyString());
            verify(emailService).enviar(eq("dra.ana@clinica.com"), anyString(), anyString());
        }

        @Test @DisplayName("notificarConsultaCancelada: render do template não estoura")
        void notificarConsultaCancelada_renderizaSemErro() {
            var consulta = consultaCompleta();
            consulta.setCancelReason("Imprevisto do paciente");

            assertDoesNotThrow(() -> emailTemplateService.notificarConsultaCancelada(consulta));

            verify(emailService, atLeastOnce()).enviar(anyString(), anyString(), anyString());
        }

        @Test @DisplayName("notificarPacienteAgendamentoAceito: render OK")
        void notificarPacienteAgendamentoAceito_renderizaSemErro() {
            assertDoesNotThrow(() ->
                emailTemplateService.notificarPacienteAgendamentoAceito(consultaCompleta())
            );
            verify(emailService).enviar(eq("paciente@email.com"), anyString(), anyString());
        }

        @Test @DisplayName("notificarPacienteAgendamentoRecusado: render OK mesmo com motivo nulo")
        void notificarPacienteAgendamentoRecusado_motivoNulo() {
            assertDoesNotThrow(() ->
                emailTemplateService.notificarPacienteAgendamentoRecusado(consultaCompleta(), null)
            );
            verify(emailService).enviar(eq("paciente@email.com"), anyString(), anyString());
        }

        @Test @DisplayName("notificarSolicitacaoAgendamentoParaMedico: render OK (chamado na mesma agendar() de produção)")
        void notificarSolicitacaoAgendamentoParaMedico_renderizaSemErro() {
            assertDoesNotThrow(() ->
                emailTemplateService.notificarSolicitacaoAgendamentoParaMedico(consultaCompleta())
            );
            verify(emailService).enviar(eq("dra.ana@clinica.com"), anyString(), anyString());
        }

        @Test @DisplayName("notificarConsultaConfirmada: render OK (paciente confirma presença)")
        void notificarConsultaConfirmada_renderizaSemErro() {
            assertDoesNotThrow(() ->
                emailTemplateService.notificarConsultaConfirmada(consultaCompleta())
            );
            verify(emailService, atLeastOnce()).enviar(anyString(), anyString(), anyString());
        }
    }

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
