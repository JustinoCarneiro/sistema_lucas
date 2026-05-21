package com.sistema.lucas.service;

import com.sistema.lucas.model.Documento;
import com.sistema.lucas.model.Patient;
import com.sistema.lucas.model.Professional;
import com.sistema.lucas.model.enums.TipoDocumento;
import com.sistema.lucas.repository.DocumentoRepository;
import com.sistema.lucas.repository.PatientRepository;
import com.sistema.lucas.repository.ProfessionalRepository;
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
class DocumentoServiceTest {

    @InjectMocks private DocumentoService documentoService;

    @Mock private DocumentoRepository documentoRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private ProfessionalRepository professionalRepository;
    @Mock private AuditLogService auditLogService;

    private Patient paciente() {
        var p = new Patient(); p.setId(1L); p.setEmail("pac@test.com"); p.setName("Paciente"); return p;
    }
    private Professional profissional() {
        var p = new Professional(); p.setId(2L); p.setEmail("prof@test.com"); p.setName("Dr. Silva"); return p;
    }

    // ──────────────────────── Busca ────────────────────────

    @Nested @DisplayName("Busca de documentos")
    class BuscaTests {

        @Test @DisplayName("buscarDosPaciente deve retornar lista e registrar auditoria")
        void buscarDosPaciente_retornaListaERegistraAuditoria() {
            var doc = new Documento();
            doc.setPaciente(paciente());
            doc.setProfissional(profissional());
            when(documentoRepository.findByPacienteEmailAndDisponivelTrueOrderByCriadoEmDesc("pac@test.com"))
                .thenReturn(List.of(doc));

            var resultado = documentoService.buscarDosPaciente("pac@test.com");

            assertEquals(1, resultado.size());
            verify(auditLogService).log(eq("pac@test.com"), eq("VISUALIZACAO_LISTA"), eq("Documento"), isNull(), anyString());
        }

        @Test @DisplayName("buscarDoProfissional deve retornar lista e registrar auditoria")
        void buscarDoProfissional_retornaListaERegistraAuditoria() {
            when(documentoRepository.findByProfissionalEmailOrderByCriadoEmDesc("prof@test.com"))
                .thenReturn(List.of());

            var resultado = documentoService.buscarDoProfissional("prof@test.com");

            assertTrue(resultado.isEmpty());
            verify(auditLogService).log(eq("prof@test.com"), eq("VISUALIZACAO_LISTA"), eq("Documento"), isNull(), anyString());
        }
    }

    // ──────────────────────── Criação ────────────────────────

    @Nested @DisplayName("Criação de documento")
    class CriacaoTests {

        @Test @DisplayName("Deve criar documento de laudo com sucesso")
        void criar_laudo_sucesso() {
            when(patientRepository.findById(1L)).thenReturn(Optional.of(paciente()));
            when(professionalRepository.findByEmail("prof@test.com")).thenReturn(Optional.of(profissional()));
            var saved = new Documento();
            saved.setId(10L);
            saved.setPaciente(paciente());
            saved.setProfissional(profissional());
            when(documentoRepository.save(any())).thenReturn(saved);

            assertDoesNotThrow(() -> documentoService.criar(1L, TipoDocumento.LAUDO_PSICOLOGICO, "Laudo",
                "Conteúdo do laudo", null, null, true, "prof@test.com"));

            verify(documentoRepository).save(any(Documento.class));
            verify(auditLogService).log(eq("prof@test.com"), eq("CRIACAO"), eq("Documento"), anyLong(), anyString());
        }

        @Test @DisplayName("Deve rejeitar arquivo com assinatura base64 inválida")
        void criar_arquivoInvalido_lancaExcecao() {
            when(patientRepository.findById(1L)).thenReturn(Optional.of(paciente()));
            when(professionalRepository.findByEmail("prof@test.com")).thenReturn(Optional.of(profissional()));

            var ex = assertThrows(RuntimeException.class, () ->
                documentoService.criar(1L, TipoDocumento.LAUDO_PSICOLOGICO, "Laudo", null, "laudo.pdf",
                    "SomethingNotPDF123==", true, "prof@test.com"));

            assertTrue(ex.getMessage().contains("Operação de Segurança"));
        }

        @Test @DisplayName("Deve lançar exceção quando paciente não encontrado")
        void criar_pacienteNaoEncontrado_lancaExcecao() {
            when(patientRepository.findById(99L)).thenReturn(Optional.empty());

            var ex = assertThrows(RuntimeException.class, () ->
                documentoService.criar(99L, TipoDocumento.LAUDO_PSICOLOGICO, "Laudo", "x", null, null, true, "prof@test.com"));

            assertTrue(ex.getMessage().contains("Paciente não encontrado"));
        }
    }

    // ──────────────────────── Exclusão ────────────────────────

    @Nested @DisplayName("Exclusão de documento")
    class ExclusaoTests {

        @Test @DisplayName("Deve lançar exceção quando profissional não é o dono")
        void excluir_profissionalErrado_lancaExcecao() {
            var doc = new Documento();
            var outro = new Professional(); outro.setEmail("outro@test.com");
            doc.setProfissional(outro);

            when(documentoRepository.findById(5L)).thenReturn(Optional.of(doc));

            var ex = assertThrows(RuntimeException.class, () ->
                documentoService.excluir(5L, "prof@test.com"));

            assertTrue(ex.getMessage().contains("permissão"));
        }

        @Test @DisplayName("Deve excluir documento com sucesso")
        void excluir_sucesso() {
            var doc = new Documento();
            doc.setProfissional(profissional());

            when(documentoRepository.findById(5L)).thenReturn(Optional.of(doc));

            assertDoesNotThrow(() -> documentoService.excluir(5L, "prof@test.com"));

            verify(documentoRepository).delete(doc);
        }
    }
}
