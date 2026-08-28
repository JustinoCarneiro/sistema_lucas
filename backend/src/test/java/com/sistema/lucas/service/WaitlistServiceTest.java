package com.sistema.lucas.service;

import com.sistema.lucas.event.ConsultaCanceladaEvent;
import com.sistema.lucas.model.Appointment;
import com.sistema.lucas.model.Patient;
import com.sistema.lucas.model.Professional;
import com.sistema.lucas.model.ProfessionalAvailability;
import com.sistema.lucas.model.WaitlistEntry;
import com.sistema.lucas.model.dto.WaitlistOfertaStatusDTO;
import com.sistema.lucas.model.enums.StatusConsulta;
import com.sistema.lucas.model.enums.WaitlistStatus;
import com.sistema.lucas.repository.AppointmentRepository;
import com.sistema.lucas.repository.PatientRepository;
import com.sistema.lucas.repository.ProfessionalAvailabilityRepository;
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
import java.time.LocalTime;
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
    @Mock private ProfessionalAvailabilityRepository availabilityRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private AppointmentService appointmentService;
    @Mock private EmailTemplateService emailTemplateService;
    @Mock private AuditLogService auditLogService;

    private Professional professional(Long id) {
        var p = new Professional();
        p.setId(id);
        p.setName("Dra. Ana");
        p.setEmail("ana@clinica.com");
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
        // self (@Lazy, usado só pra fugir de self-invocation em produção) aponta pro próprio
        // objeto sob teste — aqui não estamos testando o proxy AOP do Spring, só a lógica.
        ReflectionTestUtils.setField(waitlistService, "self", waitlistService);
    }

    private void mockSlotLivrePraOferta(Long professionalId, LocalDateTime dateTime) {
        when(appointmentService.horarioEstaOcupado(eq(professionalId), eq(dateTime))).thenReturn(false);
        var disponibilidade = new ProfessionalAvailability();
        disponibilidade.setStartTime(dateTime.toLocalTime().withMinute(0).withSecond(0).withNano(0));
        when(availabilityRepository.findByProfessionalEmailAndDate(anyString(), any()))
            .thenReturn(List.of(disponibilidade));
        // ofertarProximoDaFila reatribui `appointment` ao retorno de save() (não reusa mais a
        // instância pré-save) — bug real de produção encontrado 28/08/2026 era justamente usar a
        // referência pré-save, ver o comentário no método. O mock precisa devolver o mesmo
        // objeto recebido, senão `appointment` viraria null aqui (Mockito por padrão devolve
        // null pra métodos não stubados que retornam objeto).
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
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
            when(appointmentService.horarioEstaOcupado(1L, dateTime)).thenReturn(false);

            var ex = assertThrows(RuntimeException.class,
                () -> waitlistService.entrarNaFila(1L, dateTime, "pac@teste.com"));
            assertTrue(ex.getMessage().contains("livre"));
        }

        @Test
        @DisplayName("Entra na fila com sucesso quando o horário está ocupado")
        void entraComSucessoQuandoOcupado() {
            var dateTime = LocalDateTime.now().plusDays(1).withHour(15).withMinute(0).withSecond(0).withNano(0);

            when(professionalRepository.findById(1L)).thenReturn(Optional.of(professional(1L)));
            when(patientRepository.findByEmail("pac@teste.com")).thenReturn(Optional.of(patient(2L, "pac@teste.com")));
            when(appointmentService.horarioEstaOcupado(1L, dateTime)).thenReturn(true);
            when(waitlistEntryRepository.existsByPatientIdAndProfessionalIdAndDateTimeAndStatus(2L, 1L, dateTime, WaitlistStatus.AGUARDANDO))
                .thenReturn(false);

            assertDoesNotThrow(() -> waitlistService.entrarNaFila(1L, dateTime, "pac@teste.com"));
            verify(waitlistEntryRepository, times(1)).save(any(WaitlistEntry.class));
            verify(auditLogService, times(1)).log(eq("pac@teste.com"), eq("ENTRADA_LISTA_ESPERA"), eq("WaitlistEntry"), any(), anyString());
        }

        @Test
        @DisplayName("Recusa entrar duas vezes na fila do mesmo horário")
        void recusaDuplicata() {
            var dateTime = LocalDateTime.now().plusDays(1).withHour(15).withMinute(0).withSecond(0).withNano(0);

            when(professionalRepository.findById(1L)).thenReturn(Optional.of(professional(1L)));
            when(patientRepository.findByEmail("pac@teste.com")).thenReturn(Optional.of(patient(2L, "pac@teste.com")));
            when(appointmentService.horarioEstaOcupado(1L, dateTime)).thenReturn(true);
            when(waitlistEntryRepository.existsByPatientIdAndProfessionalIdAndDateTimeAndStatus(2L, 1L, dateTime, WaitlistStatus.AGUARDANDO))
                .thenReturn(true);

            var ex = assertThrows(RuntimeException.class,
                () -> waitlistService.entrarNaFila(1L, dateTime, "pac@teste.com"));
            assertTrue(ex.getMessage().contains("já está na lista de espera"));
        }

        @Test
        @DisplayName("Recusa paciente bloqueado por penalidade, igual a AppointmentService.agendar()")
        void recusaPacienteBloqueado() {
            var dateTime = LocalDateTime.now().plusDays(1);
            var pacienteBloqueado = patient(2L, "bloqueado@teste.com");
            pacienteBloqueado.setBlockedUntil(LocalDateTime.now().plusDays(5));

            when(professionalRepository.findById(1L)).thenReturn(Optional.of(professional(1L)));
            when(patientRepository.findByEmail("bloqueado@teste.com")).thenReturn(Optional.of(pacienteBloqueado));

            var ex = assertThrows(RuntimeException.class,
                () -> waitlistService.entrarNaFila(1L, dateTime, "bloqueado@teste.com"));
            assertTrue(ex.getMessage().contains("bloqueado"));
            verify(waitlistEntryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Reagir ao cancelamento/liberação de uma consulta (evento)")
    class AoCancelarConsultaTests {

        @Test
        @DisplayName("Chama ofertarProximoDaFila com os dados do evento")
        void chamaOfertarProximoDaFila() {
            var dateTime = LocalDateTime.now().plusDays(1);
            when(waitlistEntryRepository.findByAppointmentId(10L)).thenReturn(Optional.empty());
            when(waitlistEntryRepository.findFirstByProfessionalIdAndDateTimeAndStatusOrderByCriadoEmAsc(1L, dateTime, WaitlistStatus.AGUARDANDO))
                .thenReturn(Optional.empty());

            waitlistService.aoCancelarConsulta(new ConsultaCanceladaEvent(10L, 1L, dateTime));

            verify(waitlistEntryRepository, times(1))
                .findFirstByProfessionalIdAndDateTimeAndStatusOrderByCriadoEmAsc(1L, dateTime, WaitlistStatus.AGUARDANDO);
        }

        @Test
        @DisplayName("Sincroniza como CANCELADA uma WaitlistEntry vinculada à consulta liberada")
        void sincronizaEntradaVinculada() {
            var dateTime = LocalDateTime.now().plusDays(1);
            var entryVinculada = new WaitlistEntry();
            entryVinculada.setStatus(WaitlistStatus.CONFIRMADA);

            when(waitlistEntryRepository.findByAppointmentId(10L)).thenReturn(Optional.of(entryVinculada));
            when(waitlistEntryRepository.findFirstByProfessionalIdAndDateTimeAndStatusOrderByCriadoEmAsc(1L, dateTime, WaitlistStatus.AGUARDANDO))
                .thenReturn(Optional.empty());

            waitlistService.aoCancelarConsulta(new ConsultaCanceladaEvent(10L, 1L, dateTime));

            assertEquals(WaitlistStatus.CANCELADA, entryVinculada.getStatus());
        }

        @Test
        @DisplayName("Falha ao ofertar a vaga não propaga (efeito colateral não-crítico)")
        void falhaNaoPropaga() {
            var dateTime = LocalDateTime.now().plusDays(1);
            when(waitlistEntryRepository.findByAppointmentId(any())).thenReturn(Optional.empty());
            when(waitlistEntryRepository.findFirstByProfessionalIdAndDateTimeAndStatusOrderByCriadoEmAsc(any(), any(), any()))
                .thenThrow(new RuntimeException("falha simulada"));

            assertDoesNotThrow(() ->
                waitlistService.aoCancelarConsulta(new ConsultaCanceladaEvent(10L, 1L, dateTime)));
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
        @DisplayName("Cria a consulta reservada, marca OFERECIDA e avisa paciente e profissional")
        void ofertaParaPrimeiroDaFila() {
            var dateTime = LocalDateTime.now().plusDays(1).withHour(15).withMinute(0).withSecond(0).withNano(0);
            var entry = new WaitlistEntry();
            entry.setId(5L);
            entry.setProfessional(professional(1L));
            entry.setPatient(patient(2L, "pac@teste.com"));
            entry.setDateTime(dateTime);
            entry.setStatus(WaitlistStatus.AGUARDANDO);

            when(waitlistEntryRepository.findFirstByProfessionalIdAndDateTimeAndStatusOrderByCriadoEmAsc(1L, dateTime, WaitlistStatus.AGUARDANDO))
                .thenReturn(Optional.of(entry));
            mockSlotLivrePraOferta(1L, dateTime);

            waitlistService.ofertarProximoDaFila(1L, dateTime);

            assertEquals(WaitlistStatus.OFERECIDA, entry.getStatus());
            assertNotNull(entry.getToken());
            assertNotNull(entry.getAppointment());
            assertNotNull(entry.getOfertaExpiraEm());
            verify(appointmentRepository, times(1)).save(any(Appointment.class));
            verify(waitlistEntryRepository, times(1)).save(entry);
            verify(emailTemplateService, times(1)).notificarVagaDisponivel(entry);
            verify(emailTemplateService, times(1)).notificarSolicitacaoAgendamentoParaMedico(any(Appointment.class));
        }

        @Test
        @DisplayName("Prazo de confirmação não ultrapassa o horário da própria consulta")
        void prazoNuncaUltrapassaAConsulta() {
            var dateTime = LocalDateTime.now().plusMinutes(30); // consulta é daqui a 30min
            var entry = new WaitlistEntry();
            entry.setId(5L);
            entry.setProfessional(professional(1L));
            entry.setPatient(patient(2L, "pac@teste.com"));
            entry.setDateTime(dateTime);
            entry.setStatus(WaitlistStatus.AGUARDANDO);

            when(waitlistEntryRepository.findFirstByProfessionalIdAndDateTimeAndStatusOrderByCriadoEmAsc(1L, dateTime, WaitlistStatus.AGUARDANDO))
                .thenReturn(Optional.of(entry));
            mockSlotLivrePraOferta(1L, dateTime);

            waitlistService.ofertarProximoDaFila(1L, dateTime);

            // horasOferta = 2h (setup), mas a consulta é em 30min — o prazo tem que respeitar isso
            assertTrue(entry.getOfertaExpiraEm().isBefore(LocalDateTime.now().plusHours(1)));
        }

        @Test
        @DisplayName("Não oferece se o horário já foi ocupado por outra via (corrida com agendamento normal)")
        void naoOferecaSeHorarioJaOcupado() {
            var dateTime = LocalDateTime.now().plusDays(1);
            var entry = new WaitlistEntry();
            entry.setId(5L);
            entry.setProfessional(professional(1L));
            entry.setPatient(patient(2L, "pac@teste.com"));
            entry.setDateTime(dateTime);
            entry.setStatus(WaitlistStatus.AGUARDANDO);

            when(waitlistEntryRepository.findFirstByProfessionalIdAndDateTimeAndStatusOrderByCriadoEmAsc(1L, dateTime, WaitlistStatus.AGUARDANDO))
                .thenReturn(Optional.of(entry));
            when(appointmentService.horarioEstaOcupado(1L, dateTime)).thenReturn(true);

            waitlistService.ofertarProximoDaFila(1L, dateTime);

            verify(appointmentRepository, never()).save(any());
            verify(emailTemplateService, never()).notificarVagaDisponivel(any());
        }

        @Test
        @DisplayName("Pula entrada se o profissional não atende mais nesse horário e cascateia")
        void pulaSeProfissionalNaoAtendeMais() {
            var dateTime = LocalDateTime.now().plusDays(1).withHour(15).withMinute(0).withSecond(0).withNano(0);
            var entrySemGrade = new WaitlistEntry();
            entrySemGrade.setId(5L);
            entrySemGrade.setProfessional(professional(1L));
            entrySemGrade.setPatient(patient(2L, "pac1@teste.com"));
            entrySemGrade.setDateTime(dateTime);
            entrySemGrade.setStatus(WaitlistStatus.AGUARDANDO);

            var entryValida = new WaitlistEntry();
            entryValida.setId(6L);
            entryValida.setProfessional(professional(1L));
            entryValida.setPatient(patient(3L, "pac2@teste.com"));
            entryValida.setDateTime(dateTime);
            entryValida.setStatus(WaitlistStatus.AGUARDANDO);

            // Terceiro valor (Optional.empty) é necessário: depois de cancelar as duas entradas,
            // o while de ofertarProximoDaFila consulta de novo — sem isso, o Mockito repetiria a
            // última entrada pra sempre (ela já cancelada, mas o mock não sabe disso), travando
            // o teste num loop infinito.
            when(waitlistEntryRepository.findFirstByProfessionalIdAndDateTimeAndStatusOrderByCriadoEmAsc(1L, dateTime, WaitlistStatus.AGUARDANDO))
                .thenReturn(Optional.of(entrySemGrade), Optional.of(entryValida), Optional.empty());
            when(appointmentService.horarioEstaOcupado(1L, dateTime)).thenReturn(false);
            // profissional não atende mais nesse horário (grade vazia)
            when(availabilityRepository.findByProfessionalEmailAndDate(anyString(), any())).thenReturn(List.of());

            waitlistService.ofertarProximoDaFila(1L, dateTime);

            assertEquals(WaitlistStatus.CANCELADA, entrySemGrade.getStatus());
            assertEquals(WaitlistStatus.CANCELADA, entryValida.getStatus());
            verify(appointmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Pula paciente bloqueado por penalidade e cascateia pro próximo")
        void pulaPacienteBloqueado() {
            var dateTime = LocalDateTime.now().plusDays(1).withHour(15).withMinute(0).withSecond(0).withNano(0);

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
            mockSlotLivrePraOferta(1L, dateTime);

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
            entry.setPatient(patient(2L, "pac@teste.com"));
            entry.setOfertaExpiraEm(LocalDateTime.now().plusHours(1));
            when(waitlistEntryRepository.findByToken("token-valido")).thenReturn(Optional.of(entry));

            waitlistService.confirmarOferta("token-valido");

            assertEquals(WaitlistStatus.CONFIRMADA, entry.getStatus());
            verify(waitlistEntryRepository).save(entry);
            verify(auditLogService, times(1)).log(eq("pac@teste.com"), eq("CONFIRMACAO_VAGA_LISTA_ESPERA"), eq("WaitlistEntry"), any(), anyString());
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
        @DisplayName("Corrida com o scheduler: lock otimista vira mensagem amigável, não erro 500")
        void recusaComMensagemAmigavelSeSchedulerVenceuACorrida() {
            var entry = new WaitlistEntry();
            entry.setToken("token-corrida");
            entry.setStatus(WaitlistStatus.OFERECIDA);
            entry.setOfertaExpiraEm(LocalDateTime.now().plusMinutes(1)); // ainda não expirou pro relógio local...
            when(waitlistEntryRepository.findByToken("token-corrida")).thenReturn(Optional.of(entry));
            // ...mas o scheduler já processou e salvou esta mesma linha no banco entre a leitura
            // e o save — simulado aqui como o save lançando o conflito de versão.
            doThrow(new org.springframework.orm.ObjectOptimisticLockingFailureException(WaitlistEntry.class, 1L))
                .when(waitlistEntryRepository).save(entry);

            var ex = assertThrows(RuntimeException.class, () -> waitlistService.confirmarOferta("token-corrida"));
            assertTrue(ex.getMessage().contains("expirou"));
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
            appointment.setStatus(StatusConsulta.AGUARDANDO_CONFIRMACAO); // ainda não avançou

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

        @Test
        @DisplayName("Não cancela se a consulta já avançou pelo fluxo normal do painel — só sincroniza")
        void naoCancelaSeJaAvancouPeloFluxoNormal() {
            var dateTime = LocalDateTime.now().plusDays(1);
            var appointment = new Appointment();
            appointment.setId(99L);
            appointment.setStatus(StatusConsulta.CONFIRMADA); // paciente já confirmou pelo painel

            var entry = new WaitlistEntry();
            entry.setId(7L);
            entry.setProfessional(professional(1L));
            entry.setDateTime(dateTime);
            entry.setStatus(WaitlistStatus.OFERECIDA);
            entry.setAppointment(appointment);
            entry.setOfertaExpiraEm(LocalDateTime.now().minusMinutes(1));

            when(waitlistEntryRepository.findByStatusAndOfertaExpiraEmBefore(eq(WaitlistStatus.OFERECIDA), any()))
                .thenReturn(List.of(entry));

            waitlistService.expirarOfertasVencidas();

            assertEquals(WaitlistStatus.CONFIRMADA, entry.getStatus());
            verify(appointmentService, never()).cancelarPorExpiracaoDeOferta(any());
            verify(waitlistEntryRepository, never())
                .findFirstByProfessionalIdAndDateTimeAndStatusOrderByCriadoEmAsc(any(), any(), any());
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
            verify(auditLogService, times(1)).log(eq("pac@teste.com"), eq("SAIDA_LISTA_ESPERA"), eq("WaitlistEntry"), eq(1L), anyString());
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
