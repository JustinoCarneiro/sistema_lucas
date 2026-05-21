package com.sistema.lucas.service;

import com.sistema.lucas.model.Professional;
import com.sistema.lucas.model.dto.ProfessionalCreateDTO;
import com.sistema.lucas.model.dto.ProfessionalUpdateDTO;
import com.sistema.lucas.model.enums.TipoRegistro;
import com.sistema.lucas.repository.AppointmentRepository;
import com.sistema.lucas.repository.DocumentoRepository;
import com.sistema.lucas.repository.ProfessionalAvailabilityRepository;
import com.sistema.lucas.repository.ProfessionalRepository;
import com.sistema.lucas.repository.ProntuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class ProfessionalServiceTest {

    @InjectMocks
    private ProfessionalService professionalService;

    @Mock private ProfessionalRepository professionalRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private ProfessionalAvailabilityRepository availabilityRepository;
    @Mock private ProntuarioRepository prontuarioRepository;
    @Mock private DocumentoRepository documentoRepository;

    // ──────────────────────── Cadastro ────────────────────────

    @Test
    @DisplayName("Não deve cadastrar profissional com registro duplicado")
    void cadastrarRegistroDuplicado() {
        var dto = new ProfessionalCreateDTO(
            "Dr. House", "house@med.com", "senha123",
            TipoRegistro.CRM, "12345-SP", "Infectologia"
        );

        when(professionalRepository.existsByRegistroConselho("12345-SP")).thenReturn(true);

        var exception = assertThrows(RuntimeException.class, () -> professionalService.create(dto));
        assertTrue(exception.getMessage().contains("registro já está cadastrado"));
        verify(professionalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve cadastrar um profissional com sucesso quando os dados são válidos")
    void cadastrarComSucesso() {
        var dto = new ProfessionalCreateDTO(
            "Dr. House", "house@med.com", "senha123",
            TipoRegistro.CRM, "12345-SP", "Infectologia"
        );

        when(professionalRepository.existsByRegistroConselho(dto.registroConselho())).thenReturn(false);
        when(passwordEncoder.encode(dto.password())).thenReturn("senhaCriptografada");

        assertDoesNotThrow(() -> professionalService.create(dto));
        verify(professionalRepository, times(1)).save(any(Professional.class));
    }

    // ──────────────────────── Atualização (admin) ────────────────────────

    @Nested
    @DisplayName("update() — admin altera dados do profissional")
    class UpdateTests {

        private Professional profissionalExistente() {
            var p = new Professional();
            p.setId(1L); p.setEmail("prof@test.com"); p.setName("Dr. Original");
            p.setRegistroConselho("12345-SP");
            return p;
        }

        @Test
        @DisplayName("Deve lançar exceção ao tentar alterar para registro já usado por outro")
        void update_registroDuplicado_lancaExcecao() {
            var dto = new ProfessionalCreateDTO("Dr. House", "house@med.com", "senha123",
                TipoRegistro.CRM, "99999-RJ", "Infectologia");

            when(professionalRepository.findById(1L)).thenReturn(Optional.of(profissionalExistente()));
            when(professionalRepository.existsByRegistroConselho("99999-RJ")).thenReturn(true);

            var ex = assertThrows(RuntimeException.class, () -> professionalService.update(1L, dto));
            assertTrue(ex.getMessage().contains("registro já está cadastrado"));
        }

        @Test
        @DisplayName("Deve atualizar profissional com sucesso quando registro não está duplicado")
        void update_sucesso() {
            var dto = new ProfessionalCreateDTO("Dr. Novo", "novo@med.com", null,
                TipoRegistro.CRM, "77777-SP", "Cardiologia");

            when(professionalRepository.findById(1L)).thenReturn(Optional.of(profissionalExistente()));
            when(professionalRepository.existsByRegistroConselho("77777-SP")).thenReturn(false);

            assertDoesNotThrow(() -> professionalService.update(1L, dto));
            verify(professionalRepository).save(any(Professional.class));
        }
    }

    // ──────────────────────── Atualização de perfil ────────────────────────

    @Nested
    @DisplayName("updateMyProfile() — profissional altera próprio perfil")
    class UpdateMyProfileTests {

        private Professional profissionalPadrao() {
            var p = new Professional();
            p.setId(1L); p.setEmail("prof@test.com"); p.setName("Dr. Teste");
            p.setRegistroConselho("12345-SP");
            return p;
        }

        @Test
        @DisplayName("Deve lançar exceção quando novo e-mail já está em uso por outro profissional")
        void updateMyProfile_emailDuplicado_lancaExcecao() {
            when(professionalRepository.findByEmail("prof@test.com"))
                .thenReturn(Optional.of(profissionalPadrao()));

            var outro = new Professional(); outro.setEmail("outro@test.com");
            when(professionalRepository.findByEmail("outro@test.com"))
                .thenReturn(Optional.of(outro));

            // null=name, "outro@test.com"=email, restante null
            var dto = new ProfessionalUpdateDTO(null, "outro@test.com", null, null, null,
                null, null, null, null, null, null, null);

            var ex = assertThrows(RuntimeException.class,
                () -> professionalService.updateMyProfile("prof@test.com", dto));
            assertTrue(ex.getMessage().contains("E-mail já cadastrado"));
        }

        @Test
        @DisplayName("Deve salvar quando novo e-mail não está duplicado")
        void updateMyProfile_novoEmail_salvaComSucesso() {
            when(professionalRepository.findByEmail("prof@test.com"))
                .thenReturn(Optional.of(profissionalPadrao()));
            when(professionalRepository.findByEmail("livre@test.com"))
                .thenReturn(Optional.empty());

            var dto = new ProfessionalUpdateDTO(null, "livre@test.com", null, null, null,
                null, null, null, null, null, null, null);

            assertDoesNotThrow(() -> professionalService.updateMyProfile("prof@test.com", dto));
            verify(professionalRepository).save(any(Professional.class));
        }
    }

    // ──────────────────────── Exclusão simples ────────────────────────

    @Nested
    @DisplayName("delete() — remoção com detecção de vínculos via flush")
    class DeleteTests {

        @Test
        @DisplayName("Deve lançar exceção amigável quando profissional tem registros vinculados")
        void delete_comVinculos_lancaExcecao() {
            doThrow(new DataIntegrityViolationException("foreign key constraint"))
                .when(professionalRepository).flush();

            var ex = assertThrows(RuntimeException.class, () -> professionalService.delete(1L));
            assertTrue(ex.getMessage().contains("existem registros"));
        }

        @Test
        @DisplayName("Deve excluir sem lançar exceção quando não há vínculos")
        void delete_semVinculos_sucesso() {
            assertDoesNotThrow(() -> professionalService.delete(1L));
            verify(professionalRepository).deleteById(1L);
        }
    }

    // ──────────────────────── Exclusão forçada ────────────────────────

    @Nested
    @DisplayName("forceDelete() — cascata completa antes de excluir profissional")
    class ForceDeleteTests {

        @Test
        @DisplayName("Deve apagar documentos, prontuários, disponibilidade, consultas e depois o profissional")
        void forceDelete_cascataCompleta() {
            var prof = new Professional();
            prof.setId(1L); prof.setEmail("prof@test.com");

            when(professionalRepository.findById(1L)).thenReturn(Optional.of(prof));
            when(documentoRepository.findByProfissionalEmailOrderByCriadoEmDesc("prof@test.com"))
                .thenReturn(List.of());
            when(prontuarioRepository.findByProfessionalEmailOrderByCriadoEmDesc("prof@test.com"))
                .thenReturn(List.of());
            when(availabilityRepository.findByProfessionalId(1L)).thenReturn(List.of());
            when(appointmentRepository.findByProfessionalId(1L)).thenReturn(List.of());

            assertDoesNotThrow(() -> professionalService.forceDelete(1L));

            verify(documentoRepository).deleteAll(anyList());
            verify(prontuarioRepository).deleteAll(anyList());
            verify(availabilityRepository).deleteAll(anyList());
            verify(appointmentRepository).deleteAll(anyList());
            verify(professionalRepository).delete(prof);
        }

        @Test
        @DisplayName("Deve lançar exceção quando profissional não encontrado")
        void forceDelete_naoEncontrado_lancaExcecao() {
            when(professionalRepository.findById(99L)).thenReturn(Optional.empty());

            var ex = assertThrows(RuntimeException.class, () -> professionalService.forceDelete(99L));
            assertTrue(ex.getMessage().contains("não encontrado"));
        }
    }
}
