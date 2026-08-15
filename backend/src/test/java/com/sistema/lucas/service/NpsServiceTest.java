package com.sistema.lucas.service;

import com.sistema.lucas.model.Appointment;
import com.sistema.lucas.model.NpsResponse;
import com.sistema.lucas.model.Patient;
import com.sistema.lucas.model.Professional;
import com.sistema.lucas.model.dto.NpsStatusDTO;
import com.sistema.lucas.repository.NpsResponseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
@SuppressWarnings("null") // matchers Mockito (any()) retornam null por design
class NpsServiceTest {

    @InjectMocks
    private NpsService npsService;

    @Mock
    private NpsResponseRepository npsResponseRepository;

    @Mock
    private EmailTemplateService emailTemplateService;

    private Appointment novaConsulta(Long appointmentId) {
        var professional = new Professional();
        professional.setId(10L);
        professional.setName("Dra. Ana");

        var patient = new Patient();
        patient.setId(20L);
        patient.setName("Paciente Teste");
        patient.setEmail("paciente@teste.com");

        var appointment = new Appointment();
        appointment.setId(appointmentId);
        appointment.setProfessional(professional);
        appointment.setPatient(patient);
        appointment.setDateTime(LocalDateTime.now().minusHours(1));
        return appointment;
    }

    @Nested
    @DisplayName("Solicitar avaliação (gatilho pós-conclusão da consulta)")
    class SolicitarAvaliacaoTests {

        @Test
        @DisplayName("Cria o pedido de NPS e dispara o e-mail quando ainda não existe um pra essa consulta")
        void criaPedidoQuandoNaoExiste() {
            var appointment = novaConsulta(1L);
            when(npsResponseRepository.findByAppointmentId(1L)).thenReturn(Optional.empty());

            npsService.solicitarAvaliacao(appointment);

            verify(npsResponseRepository, times(1)).save(any(NpsResponse.class));
            verify(emailTemplateService, times(1)).solicitarAvaliacaoNps(eq(appointment), anyString());
        }

        @Test
        @DisplayName("Não duplica o pedido nem reenvia e-mail se já existe NPS pra essa consulta (idempotência)")
        void naoDuplicaQuandoJaExiste() {
            var appointment = novaConsulta(1L);
            when(npsResponseRepository.findByAppointmentId(1L)).thenReturn(Optional.of(new NpsResponse()));

            npsService.solicitarAvaliacao(appointment);

            verify(npsResponseRepository, never()).save(any());
            verify(emailTemplateService, never()).solicitarAvaliacaoNps(any(), anyString());
        }
    }

    @Nested
    @DisplayName("Responder avaliação")
    class ResponderTests {

        @Test
        @DisplayName("Registra a nota e o comentário quando o token é válido, não usado e não expirado")
        void respondeComSucesso() {
            var nps = new NpsResponse();
            nps.setToken("token-valido");
            nps.setExpiraEm(LocalDateTime.now().plusDays(1));
            when(npsResponseRepository.findByToken("token-valido")).thenReturn(Optional.of(nps));

            npsService.responder("token-valido", 9, "Ótimo atendimento");

            assertEquals(9, nps.getScore());
            assertEquals("Ótimo atendimento", nps.getComentario());
            assertNotNull(nps.getRespondidoEm());
            verify(npsResponseRepository, times(1)).save(nps);
        }

        @Test
        @DisplayName("Recusa token inexistente")
        void recusaTokenInexistente() {
            when(npsResponseRepository.findByToken("token-invalido")).thenReturn(Optional.empty());

            var exception = assertThrows(RuntimeException.class,
                () -> npsService.responder("token-invalido", 8, null));
            assertTrue(exception.getMessage().contains("inválido"));
        }

        @Test
        @DisplayName("Recusa responder duas vezes")
        void recusaSeJaRespondido() {
            var nps = new NpsResponse();
            nps.setToken("token-usado");
            nps.setExpiraEm(LocalDateTime.now().plusDays(1));
            nps.setRespondidoEm(LocalDateTime.now().minusHours(2));
            when(npsResponseRepository.findByToken("token-usado")).thenReturn(Optional.of(nps));

            var exception = assertThrows(RuntimeException.class,
                () -> npsService.responder("token-usado", 5, null));
            assertTrue(exception.getMessage().contains("já foi avaliada"));
        }

        @Test
        @DisplayName("Recusa token expirado")
        void recusaSeExpirado() {
            var nps = new NpsResponse();
            nps.setToken("token-expirado");
            nps.setExpiraEm(LocalDateTime.now().minusDays(1));
            when(npsResponseRepository.findByToken("token-expirado")).thenReturn(Optional.of(nps));

            var exception = assertThrows(RuntimeException.class,
                () -> npsService.responder("token-expirado", 5, null));
            assertTrue(exception.getMessage().contains("expirou"));
        }
    }

    @Nested
    @DisplayName("Consultar status do link")
    class ConsultarStatusTests {

        @Test
        @DisplayName("Retorna inválido quando o token não existe")
        void invalidoQuandoNaoExiste() {
            when(npsResponseRepository.findByToken("nao-existe")).thenReturn(Optional.empty());

            NpsStatusDTO status = npsService.consultarStatus("nao-existe");

            assertFalse(status.valido());
        }

        @Test
        @DisplayName("Retorna dados da consulta quando o token existe")
        void validoQuandoExiste() {
            var appointment = novaConsulta(1L);
            var nps = new NpsResponse();
            nps.setToken("token-ok");
            nps.setAppointment(appointment);
            nps.setExpiraEm(LocalDateTime.now().plusDays(1));
            when(npsResponseRepository.findByToken("token-ok")).thenReturn(Optional.of(nps));

            NpsStatusDTO status = npsService.consultarStatus("token-ok");

            assertTrue(status.valido());
            assertFalse(status.jaRespondido());
            assertFalse(status.expirado());
            assertEquals("Dra. Ana", status.profissionalNome());
        }
    }
}
