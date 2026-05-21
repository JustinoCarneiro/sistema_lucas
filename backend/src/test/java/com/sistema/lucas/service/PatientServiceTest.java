package com.sistema.lucas.service;

import com.sistema.lucas.model.Patient;
import com.sistema.lucas.model.dto.PatientCreateDTO;
import com.sistema.lucas.repository.PatientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null") // matchers Mockito (any()) retornam null por design
class PatientServiceTest {

    @InjectMocks
    private PatientService patientService;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CpfHashService cpfHashService;

    // ──────────────────────── Cadastro ────────────────────────

    @Nested
    @DisplayName("Cadastro de Pacientes")
    class CadastroTests {

        @Test
        @DisplayName("Não deve cadastrar paciente com CPF já existente (verificação via hash)")
        void cadastrarCpfDuplicado() {
            var dto = new PatientCreateDTO("Lucas Paciente", "paciente@teste.com", "123456", "111.222.333-44", "5511999998888", "Plano Saude X", true);

            // AUD-11: Verificação agora usa existsByCpfHash (consulta direta no banco)
            when(cpfHashService.hash(any())).thenReturn("hash-do-cpf");
            when(patientRepository.existsByCpfHash("hash-do-cpf")).thenReturn(true);

            var exception = assertThrows(RuntimeException.class, () -> patientService.create(dto));
            assertTrue(exception.getMessage().contains("CPF já cadastrado"));
            verify(patientRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve cadastrar um paciente com sucesso quando os dados são válidos")
        void cadastrarComSucesso() {
            var dto = new PatientCreateDTO("Novo Paciente", "novo@teste.com", "senha123", "000.000.000-00", "5511000000000", "Plano Y", true);

            // existsByCpfHash retorna false → CPF não duplicado
            when(cpfHashService.hash(any())).thenReturn("hash-novo");
            when(patientRepository.existsByCpfHash("hash-novo")).thenReturn(false);
            when(patientRepository.existsByEmail(dto.email())).thenReturn(false);
            when(passwordEncoder.encode(dto.password())).thenReturn("senhaCriptografada");

            assertDoesNotThrow(() -> patientService.create(dto));

            verify(patientRepository, times(1)).save(any(Patient.class));
            verify(patientRepository, times(1)).flush();
        }

        @Test
        @DisplayName("Cadastro concorrente: DataIntegrityViolation no flush é convertida em mensagem amigável")
        void cadastrarConcorrente_deveCapturarDataIntegrity() {
            var dto = new PatientCreateDTO("Concurrent User", "concurrent@test.com", "senha123", "999.888.777-66", "5511000000001", "Plano Z", true);

            when(cpfHashService.hash(any())).thenReturn("hash-concurrent");
            when(patientRepository.existsByCpfHash("hash-concurrent")).thenReturn(false);
            when(patientRepository.existsByEmail(dto.email())).thenReturn(false);
            when(passwordEncoder.encode(dto.password())).thenReturn("hash");
            // flush lança exceção de violação de constraint (race condition)
            doThrow(new DataIntegrityViolationException("unique constraint: unique_cpf_hash"))
                .when(patientRepository).flush();

            var exception = assertThrows(RuntimeException.class, () -> patientService.create(dto));
            assertTrue(exception.getMessage().contains("CPF ou E-mail já está em uso"));
        }
    }

    // ──────────────────────── Desbloqueio ────────────────────────

    @Nested
    @DisplayName("Desbloqueio de Pacientes")
    class DesbloqueioTests {

        @Test
        @DisplayName("Desbloquear deve zerar blockedUntil, infractionCount e receivedFirstWarning")
        void desbloquear_deveZerarInfracoesEAdvertencia() {
            var patient = new Patient();
            patient.setBlockedUntil(LocalDateTime.now().plusDays(10));
            patient.setInfractionCount(2);
            patient.setReceivedFirstWarning(true);

            when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));

            assertDoesNotThrow(() -> patientService.desbloquear(1L));

            assertNull(patient.getBlockedUntil());
            assertEquals(0, patient.getInfractionCount());
            assertFalse(patient.isReceivedFirstWarning());
            verify(patientRepository, times(1)).save(patient);
        }

        @Test
        @DisplayName("Deve lançar exceção ao tentar desbloquear paciente inexistente")
        void desbloquear_pacienteNaoEncontrado_lancaExcecao() {
            when(patientRepository.findById(99L)).thenReturn(Optional.empty());

            var ex = assertThrows(RuntimeException.class, () -> patientService.desbloquear(99L));
            assertTrue(ex.getMessage().contains("não encontrado"));
        }
    }
}
