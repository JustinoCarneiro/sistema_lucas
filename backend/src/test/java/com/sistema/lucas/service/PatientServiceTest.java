package com.sistema.lucas.service;

import com.sistema.lucas.model.Patient;
import com.sistema.lucas.model.dto.PatientCreateDTO;
import com.sistema.lucas.model.dto.PatientUpdateDTO;
import com.sistema.lucas.repository.AppointmentRepository;
import com.sistema.lucas.repository.PatientRepository;
import com.sistema.lucas.repository.ProntuarioRepository;
import com.sistema.lucas.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
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

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ProntuarioRepository prontuarioRepository;

    @Mock
    private UserRepository userRepository;

    @org.junit.jupiter.api.BeforeEach
    void setup() {
        ReflectionTestUtils.setField(patientService, "termsVersion", "1.0");
    }

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
            when(passwordEncoder.encode(dto.password())).thenReturn("hash");
            // flush lança exceção de violação de constraint (race condition)
            doThrow(new DataIntegrityViolationException("unique constraint: unique_cpf_hash"))
                .when(patientRepository).flush();

            var exception = assertThrows(RuntimeException.class, () -> patientService.create(dto));
            assertTrue(exception.getMessage().contains("CPF ou E-mail já está em uso"));
        }

        @Test
        @DisplayName("Não deve cadastrar paciente com e-mail já usado por outro usuário (ex.: profissional)")
        void cadastrarEmailDeOutroUsuario() {
            var dto = new PatientCreateDTO("Lucas Paciente", "prof@clinica.com", "senha123", "111.222.333-44", "5511999998888", "Plano X", true);

            when(cpfHashService.hash(any())).thenReturn("hash-cpf");
            when(patientRepository.existsByCpfHash("hash-cpf")).thenReturn(false);
            var prof = new com.sistema.lucas.model.User();
            prof.setEmail("prof@clinica.com");
            when(userRepository.findByEmail("prof@clinica.com")).thenReturn(prof);

            var ex = assertThrows(RuntimeException.class, () -> patientService.create(dto));
            assertTrue(ex.getMessage().contains("Email já cadastrado"));
            verify(patientRepository, never()).save(any());
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

    // ──────────────────────── Atualização de perfil ────────────────────────

    @Nested
    @DisplayName("Atualização de perfil de paciente")
    class UpdateProfileTests {

        private Patient pacientePadrao() {
            var p = new Patient();
            p.setId(1L); p.setEmail("lucas@test.com"); p.setName("Lucas"); p.setCpf("11111111111");
            return p;
        }

        @Test
        @DisplayName("Deve lançar exceção quando novo CPF já pertence a outro paciente")
        void updateMyProfile_cpfDuplicado_lancaExcecao() {
            var patient = pacientePadrao();
            when(patientRepository.findByEmail("lucas@test.com")).thenReturn(Optional.of(patient));

            // CPF diferente do atual → cpfExisteParaOutro retorna true
            when(cpfHashService.hash(any())).thenReturn("hash-novo-cpf");
            var outroPaciente = new Patient(); outroPaciente.setEmail("outro@test.com");
            when(patientRepository.findByCpfHash("hash-novo-cpf")).thenReturn(Optional.of(outroPaciente));

            var dto = new PatientUpdateDTO(null, null, "222.222.222-22", null, null, null, null, null, null, null, null);
            var ex = assertThrows(RuntimeException.class, () -> patientService.updateMyProfile("lucas@test.com", dto));
            assertTrue(ex.getMessage().contains("CPF já cadastrado"));
        }

        @Test
        @DisplayName("Deve regenerar cpfHash ao alterar CPF para um não duplicado")
        void updateMyProfile_novoCpf_regeneraHash() {
            var patient = pacientePadrao();
            when(patientRepository.findByEmail("lucas@test.com")).thenReturn(Optional.of(patient));
            when(cpfHashService.hash(any())).thenReturn("hash-33333333333");
            when(patientRepository.findByCpfHash("hash-33333333333")).thenReturn(Optional.empty());

            var dto = new PatientUpdateDTO(null, null, "333.333.333-33", null, null, null, null, null, null, null, null);
            assertDoesNotThrow(() -> patientService.updateMyProfile("lucas@test.com", dto));

            verify(cpfHashService, times(2)).hash(any()); // cpfExisteParaOutro + setCpfHash
            verify(patientRepository).save(any(Patient.class));
        }
    }

    // ──────────────────────── Exclusão LGPD ────────────────────────

    @Nested
    @DisplayName("Exclusão e anonimização LGPD")
    class ExclusaoTests {

        private Patient pacienteSemVinculos() {
            var p = new Patient(); p.setId(1L); p.setEmail("lucas@test.com"); p.setName("Lucas");
            return p;
        }

        @Test
        @DisplayName("Deve excluir fisicamente paciente sem vínculos clínicos")
        void delete_semVinculos_exclusaoFisica() {
            var patient = pacienteSemVinculos();
            when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
            when(appointmentRepository.findByPatientId(1L)).thenReturn(List.of());
            when(prontuarioRepository.findByPatientIdOrderByCriadoEmDesc(1L)).thenReturn(List.of());

            patientService.delete(1L);

            verify(patientRepository).delete(patient);
        }

        @Test
        @DisplayName("Deve anonimizar paciente com vínculos clínicos (não excluir)")
        void delete_comVinculos_anonimizacao() {
            var patient = pacienteSemVinculos();
            patient.setPassword("senha-atual");
            when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));

            // tem consultas → força anonimização
            when(appointmentRepository.findByPatientId(1L))
                .thenReturn(List.of(new com.sistema.lucas.model.Appointment()));

            when(passwordEncoder.encode(any())).thenReturn("senha-aleatoria-hash");

            patientService.delete(1L);

            // NÃO deve ter chamado delete físico
            verify(patientRepository, never()).delete(any(Patient.class));
            // Deve ter marcado como inativo e salvo
            assertFalse(patient.isActive());
            assertTrue(patient.getEmail().contains("deleted.local"));
            verify(patientRepository).save(patient);
        }
    }
}
