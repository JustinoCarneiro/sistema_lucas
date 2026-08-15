package com.sistema.lucas.service;

import com.sistema.lucas.model.Appointment;
import com.sistema.lucas.model.Patient;
import com.sistema.lucas.model.Professional;
import com.sistema.lucas.model.WaitlistEntry;
import com.sistema.lucas.model.dto.WaitlistOfertaStatusDTO;
import com.sistema.lucas.model.enums.StatusConsulta;
import com.sistema.lucas.model.enums.WaitlistStatus;
import com.sistema.lucas.repository.AppointmentRepository;
import com.sistema.lucas.repository.PatientRepository;
import com.sistema.lucas.repository.ProfessionalRepository;
import com.sistema.lucas.repository.WaitlistEntryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null") // matchers Mockito (any()) retornam null por design
class WaitlistServiceTest {

    @InjectMocks
    private WaitlistService waitlistService;

    @Mock private WaitlistEntryRepository waitlistEntryRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private ProfessionalRepository professionalRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private AppointmentService appointmentService;
    @Mock private EmailTemplateService emailTemplateService;

    private Professional professional(Long id) {
        var p = new Professional();
        p.setId(id);
        p.setName("Dra. Ana");
        return p;
    }

    private Patient patient(Long id, String email) {
        var p = new Patient();
        p.setId(id);
        p.setEmail(email);
        p.setName("Paciente Teste");
        return p;
    }

    @org.junit.jupiter.api.BeforeEach
    void setup() {
        ReflectionTestUtils.setField(waitlistService, "horasOferta", 2L);
    }

    @Nested
    @DisplayName("Entrar na fila")
    class EntrarNaFilaTests {

        @Test
        @DisplayName("Recusa entrar na fila se o horário está livre")
        void recusaSeHorarioLivre() {
            var dateTime = LocalDateTime.now().plusDays(1);
            when(professionalRepository.findById(1L)).thenReturn(Optional.of(professional(1L)));
            when(patientRepository.findByEmail("pac@teste.com")).thenReturn(Optional.of(patient(2L, "pac@teste.com")));
            when(appointmentRepository.findByProfessionalIdAndDateTimeBetweenAndStatusNot(eq(1L), any(), any(), any()))
                .thenReturn(List.of());

            var ex = assertThrows(RuntimeException.class,
                () -> waitlistService.entrarNaFila(1L, dateTime, "pac@teste.com"));
            assertTrue(ex.getMessage().contains("livre"));
        }

        @Test
        @DisplayName("Entra na fila com sucesso quando o horário está ocupado")
        void entraComSucessoQuandoOcupado() {
            var dateTime = LocalDateTime.now().plusDays(1).withHour(15).withMinute(0).withSecond(0).withNano(0);
            var ocupante = new Appointment();
            ocupante.setDateTime(dateTime);
            ocupante.setStatus(StatusConsulta.AGENDADA);

            when(professionalRepository.findById(1L)).thenReturn(Optional.of(professional(1L)));
            when(patientRepository.findByEmail("pac@teste.com")).thenReturn(Optional.of(patient(2L, "pac@teste.com")));
            when(appointmentRepository.findByProfessionalIdAndDateTimeBetweenAndStatusNot(eq(1L), any(), any(), any()))
                .thenReturn(List.of(ocupante));
            when(waitlistEntryRepository.existsByPatientIdAndProfessionalIdAndDateTimeAndStatus(2L, 1L, dateTime, WaitlistStatus.AGUARDANDO))
                .thenReturn(false);

            assertDoesNotThrow(() -> waitlistService.entrarNaFila(1L, dateTime, "pac@teste.com"));
            verify(waitlistEntryRepository, times(1)).save(any(WaitlistEntry.class));
        }

        @Test
        @DisplayName("Recusa entrar duas vezes na fila do mesmo horário")
        void recusaDuplicata() {
            var dateTime = LocalDateTime.now().plusDays(1).withHour(15).withMinute(0).withSecond(0).withNano(0);
            var ocupante = new Appointment();
            ocupante.setDateTime(dateTime);
            ocupante.setStatus(StatusConsulta.AGENDADA);

            when(professionalRepository.findById(1L)).thenReturn(Optional.of(professional(1L)));
            when(patientRepository.findByEmail("pac@teste.com")).thenReturn(Optional.of(patient(2L, "pac@teste.com")));
            when(appointmentRepository.findByProfessionalIdAndDateTimeBetweenAndStatusNot(eq(1L), any(), any(), any()))
                .thenReturn(List.of(ocupante));
            when(waitlistEntryRepository.existsByPatientIdAndProfessionalIdAndDateTimeAndStatus(2L, 1L, dateTime, WaitlistStatus.AGUARDANDO))
                .thenReturn(true);

            var ex = assertThrows(RuntimeException.class,
                () -> waitlistService.entrarNaFila(1L, dateTime, "pac@teste.com"));
            assertTrue(ex.getMessage().contains("já está na lista de espera"));
        }
    }

    @Nested
    @DisplayName("Reagir ao cancelamento de consulta (evento)")
    class AoCancelarConsultaTests {

        @Test
        @DisplayName("Chama ofertarProximoDaFila com os dados do evento")
        void chamaOfertarProximoDaFila() {
            var dateTime = LocalDateTime.now().plusDays(1);
            when(waitlistEntryRepository.findFirstByProfessionalIdAndDateTimeAndStatusOrderByCriadoEmAsc(1L, dateTime, WaitlistStatus.AGUARDANDO))
                .thenReturn(Optional.empty());

            waitlistService.aoCancelarConsulta(new com.sistema.lucas.event.ConsultaCanceladaEvent(1L, dateTime));

            verify(waitlistEntryRepository, times(1))
                .findFirstByProfessionalIdAndDateTimeAndStatusOrderByCriadoEmAsc(1L, dateTime, WaitlistStatus.AGUARDANDO);
        }

        @Test
        @DisplayName("Falha ao ofertar a vaga não propaga (efeito colateral não-crítico)")
        void falhaNaoPropaga() {
            var dateTime = LocalDateTime.now().plusDays(1);
            when(waitlistEntryRepository.findFirstByProfessionalIdAndDateTimeAndStatusOrderByCriadoEmAsc(any(), any(), any()))
                .thenThrow(new RuntimeException("falha simulada"));

            assertDoesNotThrow(() ->
                waitlistService.aoCancelarConsulta(new com.sistema.lucas.event.ConsultaCanceladaEvent(1L, dateTime)));
        }
    }

    @Nested
    @DisplayName("Ofertar vaga ao próximo da fila")
    class OfertarProximoTests {

        @Test
        @DisplayName("Não faz nada quando a fila está vazia")
        void naoFazNadaQuandoVazia() {
            when(waitlistEntryRepository.findFirstByProfessionalIdAndDateTimeAndStatusOrderByCriadoEmAsc(any(), any(), any()))
                .thenReturn(Optional.empty());

            waitlistService.ofertarProximoDaFila(1L, LocalDateTime.now());

            verify(appointmentRepository, never()).save(any());
            verify(emailTemplateService, never()).notificarVagaDisponivel(any());
        }

        @Test
        @DisplayName("Cria a consulta reservada, marca OFERECIDA e envia e-mail pro primeiro da fila")
        void ofertaParaPrimeiroDaFila() {
            var dateTime = LocalDateTime.now().plusDays(1);
            var entry = new WaitlistEntry();
            entry.setId(5L);
            entry.setProfessional(professional(1L));
            entry.setPatient(patient(2L, "pac@teste.com"));
            entry.setDateTime(dateTime);
            entry.setStatus(WaitlistStatus.AGUARDANDO);

            when(waitlistEntryRepository.findFirstByProfessionalIdAndDateTimeAndStatusOrderByCriadoEmAsc(1L, dateTime, WaitlistStatus.AGUARDANDO))
                .thenReturn(Optional.of(entry));

            waitlistService.ofertarProximoDaFila(1L, dateTime);

            assertEquals(WaitlistStatus.OFERECIDA, entry.getStatus());
            assertNotNull(entry.getToken());
            assertNotNull(entry.getAppointment());
            assertNotNull(entry.getOfertaExpiraEm());
            verify(appointmentRepository, times(1)).save(any(Appointment.class));
            verify(waitlistEntryRepository, times(1)).save(entry);
            verify(emailTemplateService, times(1)).notificarVagaDisponivel(entry);
        }

        @Test
        @DisplayName("Pula paciente bloqueado por penalidade e cascateia pro próximo")
        void pulaPacienteBloqueado() {
            var dateTime = LocalDateTime.now().plusDays(1);

            var pacienteBloqueado = patient(2L, "bloqueado@teste.com");
            pacienteBloqueado.setBlockedUntil(LocalDateTime.now().plusDays(5));
            var entryBloqueado = new WaitlistEntry();
            entryBloqueado.setId(5L);
            entryBloqueado.setProfessional(professional(1L));
            entryBloqueado.setPatient(pacienteBloqueado);
            entryBloqueado.setDateTime(dateTime);
            entryBloqueado.setStatus(WaitlistStatus.AGUARDANDO);

            var entryValido = new WaitlistEntry();
            entryValido.setId(6L);
            entryValido.setProfessional(professional(1L));
            entryValido.setPatient(patient(3L, "livre@teste.com"));
            entryValido.setDateTime(dateTime);
            entryValido.setStatus(WaitlistStatus.AGUARDANDO);

            when(waitlistEntryRepository.findFirstByProfessionalIdAndDateTimeAndStatusOrderByCriadoEmAsc(1L, dateTime, WaitlistStatus.AGUARDANDO))
                .thenReturn(Optional.of(entryBloqueado), Optional.of(entryValido));

            waitlistService.ofertarProximoDaFila(1L, dateTime);

            assertEquals(WaitlistStatus.CANCELADA, entryBloqueado.getStatus());
            assertEquals(WaitlistStatus.OFERECIDA, entryValido.getStatus());
            verify(emailTemplateService, times(1)).notificarVagaDisponivel(entryValido);
        }
    }

    @Nested
    @DisplayName("Confirmar oferta")
    class ConfirmarOfertaTests {

        @Test
        @DisplayName("Confirma com sucesso dentro do prazo")
        void confirmaComSucesso() {
            var entry = new WaitlistEntry();
            entry.setToken("token-valido");
            entry.setStatus(WaitlistStatus.OFERECIDA);
            entry.setOfertaExpiraEm(LocalDateTime.now().plusHours(1));
            when(waitlistEntryRepository.findByToken("token-valido")).thenReturn(Optional.of(entry));

            waitlistService.confirmarOferta("token-valido");

            assertEquals(WaitlistStatus.CONFIRMADA, entry.getStatus());
            verify(waitlistEntryRepository).save(entry);
        }

        @Test
        @DisplayName("Recusa token inexistente")
        void recusaTokenInvalido() {
            when(waitlistEntryRepository.findByToken("invalido")).thenReturn(Optional.empty());
            assertThrows(RuntimeException.class, () -> waitlistService.confirmarOferta("invalido"));
        }

        @Test
        @DisplayName("Recusa confirmar duas vezes")
        void recusaSeJaConfirmada() {
            var entry = new WaitlistEntry();
            entry.setToken("token-usado");
            entry.setStatus(WaitlistStatus.CONFIRMADA);
            when(waitlistEntryRepository.findByToken("token-usado")).thenReturn(Optional.of(entry));

            var ex = assertThrows(RuntimeException.class, () -> waitlistService.confirmarOferta("token-usado"));
            assertTrue(ex.getMessage().contains("já confirmou"));
        }

        @Test
        @DisplayName("Recusa confirmar oferta expirada")
        void recusaSeExpirada() {
            var entry = new WaitlistEntry();
            entry.setToken("token-expirado");
            entry.setStatus(WaitlistStatus.OFERECIDA);
            entry.setOfertaExpiraEm(LocalDateTime.now().minusMinutes(1));
            when(waitlistEntryRepository.findByToken("token-expirado")).thenReturn(Optional.of(entry));

            var ex = assertThrows(RuntimeException.class, () -> waitlistService.confirmarOferta("token-expirado"));
            assertTrue(ex.getMessage().contains("expirou"));
        }
    }

    @Nested
    @DisplayName("Consultar status da oferta")
    class ConsultarOfertaTests {

        @Test
        @DisplayName("Token inexistente retorna inválido")
        void invalidoQuandoNaoExiste() {
            when(waitlistEntryRepository.findByToken("nada")).thenReturn(Optional.empty());
            WaitlistOfertaStatusDTO status = waitlistService.consultarOferta("nada");
            assertFalse(status.valido());
        }
    }

    @Nested
    @DisplayName("Expirar ofertas vencidas")
    class ExpirarOfertasTests {

        @Test
        @DisplayName("Cancela a consulta reservada, marca EXPIRADA e cascateia pro próximo")
        void expiraECascateia() {
            var dateTime = LocalDateTime.now().plusDays(1);
            var appointment = new Appointment();
            appointment.setId(99L);

            var entry = new WaitlistEntry();
            entry.setId(7L);
            entry.setProfessional(professional(1L));
            entry.setDateTime(dateTime);
            entry.setStatus(WaitlistStatus.OFERECIDA);
            entry.setAppointment(appointment);
            entry.setOfertaExpiraEm(LocalDateTime.now().minusMinutes(1));

            when(waitlistEntryRepository.findByStatusAndOfertaExpiraEmBefore(eq(WaitlistStatus.OFERECIDA), any()))
                .thenReturn(List.of(entry));
            when(waitlistEntryRepository.findFirstByProfessionalIdAndDateTimeAndStatusOrderByCriadoEmAsc(1L, dateTime, WaitlistStatus.AGUARDANDO))
                .thenReturn(Optional.empty());

            waitlistService.expirarOfertasVencidas();

            assertEquals(WaitlistStatus.EXPIRADA, entry.getStatus());
            verify(appointmentService, times(1)).cancelarPorExpiracaoDeOferta(99L);
        }
    }

    @Nested
    @DisplayName("Sair da fila")
    class SairDaFilaTests {

        @Test
        @DisplayName("Remove com sucesso quando o dono está aguardando")
        void removeComSucesso() {
            var entry = new WaitlistEntry();
            entry.setId(1L);
            entry.setPatient(patient(2L, "pac@teste.com"));
            entry.setStatus(WaitlistStatus.AGUARDANDO);
            when(waitlistEntryRepository.findById(1L)).thenReturn(Optional.of(entry));

            waitlistService.sairDaFila(1L, "pac@teste.com");

            assertEquals(WaitlistStatus.CANCELADA, entry.getStatus());
        }

        @Test
        @DisplayName("Recusa remover entrada de outro paciente")
        void recusaDonoErrado() {
            var entry = new WaitlistEntry();
            entry.setId(1L);
            entry.setPatient(patient(2L, "dono@teste.com"));
            entry.setStatus(WaitlistStatus.AGUARDANDO);
            when(waitlistEntryRepository.findById(1L)).thenReturn(Optional.of(entry));

            assertThrows(RuntimeException.class, () -> waitlistService.sairDaFila(1L, "outro@teste.com"));
        }

        @Test
        @DisplayName("Recusa sair da fila se a vaga já foi ofertada")
        void recusaSeJaOferecida() {
            var entry = new WaitlistEntry();
            entry.setId(1L);
            entry.setPatient(patient(2L, "pac@teste.com"));
            entry.setStatus(WaitlistStatus.OFERECIDA);
            when(waitlistEntryRepository.findById(1L)).thenReturn(Optional.of(entry));

            assertThrows(RuntimeException.class, () -> waitlistService.sairDaFila(1L, "pac@teste.com"));
        }
    }
}
