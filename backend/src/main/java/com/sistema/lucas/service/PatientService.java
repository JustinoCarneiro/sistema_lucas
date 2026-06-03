package com.sistema.lucas.service;

import com.sistema.lucas.model.Patient;
import com.sistema.lucas.model.dto.PatientCreateDTO;
import com.sistema.lucas.model.enums.Role;
import com.sistema.lucas.repository.AppointmentRepository;
import com.sistema.lucas.repository.PatientRepository;
import com.sistema.lucas.repository.UserRepository;
import com.sistema.lucas.repository.ProntuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sistema.lucas.model.dto.PatientUpdateDTO;

import java.util.List;

@Service
public class PatientService {

    @Autowired
    private PatientRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private ProntuarioRepository prontuarioRepository;

    @Autowired
    private CpfHashService cpfHashService;

    @Autowired
    private UserRepository userRepository;

    // LGPD — versão vigente dos termos, registrada junto ao consentimento.
    @org.springframework.beans.factory.annotation.Value("${app.lgpd.terms-version}")
    private String termsVersion;

    public List<Patient> findAll() {
        return repository.findAll();
    }

    // AUD-11: Verificação de CPF via hash no banco (sem carregar todos em memória)
    private boolean cpfExiste(String cpf) {
        String hash = cpfHashService.hash(cpf);
        return repository.existsByCpfHash(hash);
    }

    private boolean cpfExisteParaOutro(String cpf, String emailAtual) {
        String hash = cpfHashService.hash(cpf);
        return repository.findByCpfHash(hash)
            .map(p -> !p.getEmail().equals(emailAtual))
            .orElse(false);
    }

    @Transactional
    public void create(PatientCreateDTO dto) {
        // LGPD — o cadastro só prossegue mediante consentimento expresso.
        if (!dto.termsAccepted()) {
            throw new RuntimeException("Erro: é necessário aceitar os Termos de Uso e a Política de Privacidade.");
        }
        if (cpfExiste(dto.cpf())) {
            throw new RuntimeException("Erro: CPF já cadastrado.");
        }
        var email = dto.email().trim().toLowerCase();
        if (userRepository.findByEmail(email) != null) { // checa em TODOS os usuários, não só na tabela patient
            throw new RuntimeException("Erro: Email já cadastrado.");
        }

        // MAPEAMENTO MANUAL: Sem usar o construtor customizado
        Patient patient = new Patient();
        patient.setName(dto.name());
        patient.setEmail(email);
        patient.setPassword(passwordEncoder.encode(dto.password()));
        patient.setRole(Role.PATIENT);
        patient.setCpf(dto.cpf());
        // AUD-03: Hash HMAC-SHA256 com pepper (substituindo SHA-256 puro)
        patient.setCpfHash(cpfHashService.hash(dto.cpf()));
        // LGPD — consentimento registrado com prova demonstrável (Art. 8º §1)
        patient.setTermsAccepted(true);
        patient.setTermsAcceptedAt(java.time.LocalDateTime.now());
        patient.setTermsVersion(termsVersion);

        try {
            repository.save(patient);
            repository.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new RuntimeException("Erro: Este CPF ou E-mail já está em uso por outra conta.");
        }
    }

    // LGPD — Direito ao esquecimento
    // Quando houver vínculos clínicos (consultas/prontuários/documentos), o paciente
    // NÃO pode ser apagado fisicamente, pois o CFM exige a retenção de prontuários
    // por 20 anos. Nestes casos, executamos uma anonimização irreversível dos PII.
    @Transactional
    public void delete(@org.springframework.lang.NonNull Long id) {
        Patient patient = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));
        deleteOrAnonymize(patient);
    }

    public Patient getMyProfile(String email) {
        return repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));
    }

    @Transactional
    public void deleteByEmail(String email) {
        Patient patient = repository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));
        deleteOrAnonymize(patient);
    }

    /**
     * Decide entre exclusão física e anonimização (soft-delete) de acordo com a
     * existência de vínculos clínicos. Anonimização aplica valores fictícios
     * irreversíveis aos PII e marca {@code isActive = false}, preservando os
     * registros clínicos vinculados.
     */
    private void deleteOrAnonymize(Patient patient) {
        if (temVinculosClinicos(patient.getId())) {
            anonymize(patient);
            return;
        }
        try {
            repository.delete(patient);
            repository.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Caso surja um vínculo concorrente entre a checagem e o delete,
            // recuamos para anonimização para nunca violar a integridade.
            anonymize(patient);
        }
    }

    private boolean temVinculosClinicos(Long patientId) {
        if (!appointmentRepository.findByPatientId(patientId).isEmpty()) return true;
        if (!prontuarioRepository.findByPatientIdOrderByCriadoEmDesc(patientId).isEmpty()) return true;
        // Documentos não possuem método por paciente neste repositório, mas a
        // FK paciente_id em Documento ativa a verificação na hora do delete.
        // Caso queiramos cobertura proativa, o catch acima trata o fallback.
        return false;
    }

    private void anonymize(Patient patient) {
        Long id = patient.getId();
        String stamp = "anonymized-" + id;

        // PII textuais — substituídos por placeholders irreversíveis
        patient.setName("Paciente Anonimizado");
        patient.setEmail(stamp + "@deleted.local");
        patient.setPhone(null);
        patient.setAddress(null);
        patient.setAllergies(null);
        patient.setEmergencyContactName(null);
        patient.setEmergencyContactPhone(null);
        patient.setBirthDate(null);
        patient.setGender(null);

        // CPF: limpamos o valor original e geramos um cpf_hash placeholder único
        // (a coluna é UNIQUE NOT NULL — não podemos zerá-la).
        patient.setCpf(null);
        patient.setCpfHash(stamp);

        // Credenciais: senha aleatória + e-mail não verificado.
        // O usuário fica impossibilitado de autenticar (alvo de DPO/Admin).
        patient.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
        patient.setVerified(false);

        // Flag LGPD de soft-delete
        patient.setActive(false);

        // Limpa também o histórico de penalidades — não é mais um sujeito de dados ativo.
        patient.setBlockedUntil(null);
        patient.setInfractionCount(0);
        patient.setReceivedFirstWarning(false);

        repository.save(patient);
        repository.flush();
    }

    // Adicionar ao PatientService.java
    @Transactional
    public void updateMyProfile(String email, PatientUpdateDTO dto) {
        Patient patient = repository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));

        if (dto.name() != null && !dto.name().trim().isEmpty()) {
            patient.setName(dto.name());
        }

        if (dto.email() != null && !dto.email().trim().isEmpty()) {
            var novoEmail = dto.email().trim().toLowerCase();
            if (!novoEmail.equals(patient.getEmail())) {
                if (userRepository.findByEmail(novoEmail) != null) {
                    throw new RuntimeException("Erro: E-mail já cadastrado por outro usuário.");
                }
                patient.setEmail(novoEmail);
            }
        }

        if (dto.cpf() != null && !dto.cpf().trim().isEmpty() && !dto.cpf().equals(patient.getCpf())) {
            if (cpfExisteParaOutro(dto.cpf(), patient.getEmail())) {
                throw new RuntimeException("Erro: CPF já cadastrado por outro usuário.");
            }
            patient.setCpf(dto.cpf());
            // AUD-03: Atualizar o hash HMAC ao alterar o CPF
            patient.setCpfHash(cpfHashService.hash(dto.cpf()));
        }

        if (dto.phone() != null) patient.setPhone(dto.phone());
        if (dto.birthDate() != null) patient.setBirthDate(dto.birthDate());
        if (dto.emergencyContactName() != null) patient.setEmergencyContactName(dto.emergencyContactName());
        if (dto.emergencyContactPhone() != null) patient.setEmergencyContactPhone(dto.emergencyContactPhone());
        if (dto.gender() != null) patient.setGender(dto.gender());
        if (dto.allergies() != null) patient.setAllergies(dto.allergies());
        if (dto.address() != null) patient.setAddress(dto.address());

        if (dto.newPassword() != null && !dto.newPassword().trim().isEmpty()) {
            patient.setPassword(passwordEncoder.encode(dto.newPassword()));
        }

        repository.save(java.util.Objects.requireNonNull(patient));
    }

    @Transactional
    public void desbloquear(@org.springframework.lang.NonNull Long id) {
        Patient patient = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));
        // O desbloqueio administrativo zera todo o histórico de penalidades:
        // libera o agendamento e dá ao paciente um recomeço limpo.
        patient.setBlockedUntil(null);
        patient.setInfractionCount(0);
        patient.setReceivedFirstWarning(false);
        repository.save(patient);
    }
}
