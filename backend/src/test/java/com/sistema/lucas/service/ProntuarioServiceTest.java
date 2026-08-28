package com.sistema.lucas.service;

import com.sistema.lucas.model.Appointment;
import com.sistema.lucas.model.Patient;
import com.sistema.lucas.model.Professional;
import com.sistema.lucas.model.Prontuario;
import com.sistema.lucas.model.User;
import com.sistema.lucas.model.enums.Role;
import com.sistema.lucas.model.enums.StatusConsulta;
import com.sistema.lucas.repository.AppointmentRepository;
import com.sistema.lucas.repository.ProfessionalRepository;
import com.sistema.lucas.repository.ProntuarioRepository;
import com.sistema.lucas.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class ProntuarioServiceTest {

    @InjectMocks private ProntuarioService prontuarioService;

    @Mock private ProntuarioRepository prontuarioRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private ProfessionalRepository professionalRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private NpsService npsService;

    private Appointment appointmentValido() {
        var patient = new Patient(); patient.setId(1L); patient.setEmail("pac@test.com");
        var prof = new Professional(); prof.setEmail("prof@test.com");
        var a = new Appointment();
        a.setId(10L); a.setPatient(patient); a.setProfessional(prof);
        a.setStatus(StatusConsulta.CONFIRMADA);
        return a;
    }

    // ──────────────────────── Criação ────────────────────────

    @Nested @DisplayName("Criação de prontuário")
    class CriacaoTests {

        @Test @DisplayName("Deve criar evolução de prontuário com sucesso")
        void criar_sucesso() {
            var appointment = appointmentValido();
            var prof = new Professional(); prof.setEmail("prof@test.com");
            var saved = new Prontuario(); saved.setId(5L);

            when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));
            when(professionalRepository.findByEmail("prof@test.com")).thenReturn(Optional.of(prof));
            when(prontuarioRepository.save(any())).thenReturn(saved);

            var result = prontuarioService.create(10L, "Notas clínicas...", "prof@test.com");

            assertNotNull(result);
            assertEquals(StatusConsulta.CONCLUIDA, appointment.getStatus());
            verify(appointmentRepository).save(appointment);
            verify(prontuarioRepository).save(any(Prontuario.class));
            verify(auditLogService).log(eq("prof@test.com"), eq("CRIACAO"), eq("Prontuario"), anyLong(), anyString());
            verify(npsService).solicitarAvaliacao(appointment);
        }

        @Test @DisplayName("Falha ao solicitar NPS não impede a criação do prontuário (efeito colateral não-crítico)")
        void criar_falhaNoNpsNaoQuebraCriacaoDoProntuario() {
            var appointment = appointmentValido();
            var prof = new Professional(); prof.setEmail("prof@test.com");
            var saved = new Prontuario(); saved.setId(5L);

            when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));
            when(professionalRepository.findByEmail("prof@test.com")).thenReturn(Optional.of(prof));
            when(prontuarioRepository.save(any())).thenReturn(saved);
            doThrow(new RuntimeException("falha simulada no envio do NPS"))
                .when(npsService).solicitarAvaliacao(any());

            var result = prontuarioService.create(10L, "Notas clínicas...", "prof@test.com");

            assertNotNull(result);
            assertEquals(StatusConsulta.CONCLUIDA, appointment.getStatus());
        }

        @Test @DisplayName("Deve lançar exceção quando consulta não encontrada")
        void criar_consultaNaoEncontrada_lancaExcecao() {
            when(appointmentRepository.findById(99L)).thenReturn(Optional.empty());

            var ex = assertThrows(RuntimeException.class, () ->
                prontuarioService.create(99L, "Notas", "prof@test.com"));

            assertTrue(ex.getMessage().contains("Consulta não encontrada"));
        }

        @Test @DisplayName("Deve lançar exceção quando profissional não encontrado")
        void criar_profissionalNaoEncontrado_lancaExcecao() {
            // Dono da consulta bate com quem está chamando (passa pela guarda de posse), mas o
            // e-mail não existe em ProfessionalRepository — inconsistência de dados isolada.
            var appointment = appointmentValido();
            appointment.getProfessional().setEmail("orfao@test.com");
            when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));
            when(professionalRepository.findByEmail("orfao@test.com")).thenReturn(Optional.empty());

            var ex = assertThrows(RuntimeException.class, () ->
                prontuarioService.create(10L, "Notas", "orfao@test.com"));

            assertTrue(ex.getMessage().contains("Profissional não encontrado"));
        }

        @Test @DisplayName("Deve lançar exceção quando quem chama não é o profissional dono da consulta")
        void criar_naoEhDonoDaConsulta_lancaExcecao() {
            when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointmentValido()));

            var ex = assertThrows(RuntimeException.class, () ->
                prontuarioService.create(10L, "Notas", "desconhecido@test.com"));

            assertTrue(ex.getMessage().contains("não é o profissional"));
        }
    }

    // ──────────────────────── Busca ────────────────────────

    @Nested @DisplayName("Busca de prontuário")
    class BuscaTests {

        private User professionalUser(String email) {
            var u = new User(); u.setEmail(email); u.setRole(Role.PROFESSIONAL);
            return u;
        }

        @Test @DisplayName("getByPatientId deve retornar lista e registrar auditoria")
        void getByPatientId_retornaListaERegistraAuditoria() {
            var prontuario = new Prontuario();
            when(userRepository.findByEmail("prof@test.com")).thenReturn(professionalUser("prof@test.com"));
            when(appointmentRepository.existsByProfessionalEmailAndPatientId("prof@test.com", 1L)).thenReturn(true);
            when(prontuarioRepository.findByPatientIdOrderByCriadoEmDesc(1L)).thenReturn(List.of(prontuario));

            var resultado = prontuarioService.getByPatientId(1L, "prof@test.com");

            assertEquals(1, resultado.size());
            verify(auditLogService).log(eq("prof@test.com"), eq("VISUALIZACAO"), eq("Prontuario"), eq(1L), anyString());
        }

        @Test @DisplayName("getByPatientId deve retornar lista vazia quando sem prontuários")
        void getByPatientId_semRegistros_retornaVazio() {
            when(userRepository.findByEmail("prof@test.com")).thenReturn(professionalUser("prof@test.com"));
            when(appointmentRepository.existsByProfessionalEmailAndPatientId("prof@test.com", 1L)).thenReturn(true);
            when(prontuarioRepository.findByPatientIdOrderByCriadoEmDesc(1L)).thenReturn(List.of());

            var resultado = prontuarioService.getByPatientId(1L, "prof@test.com");

            assertTrue(resultado.isEmpty());
        }

        @Test @DisplayName("getByPatientId nega acesso a profissional que nunca atendeu o paciente (IDOR)")
        void getByPatientId_profissionalSemVinculo_lancaExcecao() {
            when(userRepository.findByEmail("prof@test.com")).thenReturn(professionalUser("prof@test.com"));
            when(appointmentRepository.existsByProfessionalEmailAndPatientId("prof@test.com", 1L)).thenReturn(false);

            var ex = assertThrows(RuntimeException.class, () ->
                prontuarioService.getByPatientId(1L, "prof@test.com"));

            assertTrue(ex.getMessage().contains("Acesso negado"));
            verifyNoInteractions(auditLogService);
        }

        @Test @DisplayName("getByPatientId permite ADMIN ver qualquer paciente sem checar vínculo")
        void getByPatientId_admin_naoChecaVinculo() {
            var admin = new User(); admin.setEmail("admin@test.com"); admin.setRole(Role.ADMIN);
            when(userRepository.findByEmail("admin@test.com")).thenReturn(admin);
            when(prontuarioRepository.findByPatientIdOrderByCriadoEmDesc(1L)).thenReturn(List.of(new Prontuario()));

            var resultado = prontuarioService.getByPatientId(1L, "admin@test.com");

            assertEquals(1, resultado.size());
            verify(appointmentRepository, never()).existsByProfessionalEmailAndPatientId(any(), any());
        }

        @Test @DisplayName("getByPatientId permite TECNICO ver qualquer paciente sem checar vínculo (mesmo nível do ADMIN)")
        void getByPatientId_tecnico_naoChecaVinculo() {
            var tecnico = new User(); tecnico.setEmail("tecnico@test.com"); tecnico.setRole(Role.TECNICO);
            when(userRepository.findByEmail("tecnico@test.com")).thenReturn(tecnico);
            when(prontuarioRepository.findByPatientIdOrderByCriadoEmDesc(1L)).thenReturn(List.of(new Prontuario()));

            var resultado = prontuarioService.getByPatientId(1L, "tecnico@test.com");

            assertEquals(1, resultado.size());
            verify(appointmentRepository, never()).existsByProfessionalEmailAndPatientId(any(), any());
        }
    }
}
