package com.sistema.lucas.service;

import com.sistema.lucas.model.Appointment;
import com.sistema.lucas.model.Patient;
import com.sistema.lucas.model.Professional;
import com.sistema.lucas.model.Prontuario;
import com.sistema.lucas.model.enums.StatusConsulta;
import com.sistema.lucas.repository.AppointmentRepository;
import com.sistema.lucas.repository.ProfessionalRepository;
import com.sistema.lucas.repository.ProntuarioRepository;
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
    @Mock private AuditLogService auditLogService;

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
            when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointmentValido()));
            when(professionalRepository.findByEmail("desconhecido@test.com")).thenReturn(Optional.empty());

            var ex = assertThrows(RuntimeException.class, () ->
                prontuarioService.create(10L, "Notas", "desconhecido@test.com"));

            assertTrue(ex.getMessage().contains("Profissional não encontrado"));
        }
    }

    // ──────────────────────── Busca ────────────────────────

    @Nested @DisplayName("Busca de prontuário")
    class BuscaTests {

        @Test @DisplayName("getByPatientId deve retornar lista e registrar auditoria")
        void getByPatientId_retornaListaERegistraAuditoria() {
            var prontuario = new Prontuario();
            when(prontuarioRepository.findByPatientIdOrderByCriadoEmDesc(1L)).thenReturn(List.of(prontuario));

            var resultado = prontuarioService.getByPatientId(1L, "prof@test.com");

            assertEquals(1, resultado.size());
            verify(auditLogService).log(eq("prof@test.com"), eq("VISUALIZACAO"), eq("Prontuario"), eq(1L), anyString());
        }

        @Test @DisplayName("getByPatientId deve retornar lista vazia quando sem prontuários")
        void getByPatientId_semRegistros_retornaVazio() {
            when(prontuarioRepository.findByPatientIdOrderByCriadoEmDesc(1L)).thenReturn(List.of());

            var resultado = prontuarioService.getByPatientId(1L, "prof@test.com");

            assertTrue(resultado.isEmpty());
        }
    }
}
