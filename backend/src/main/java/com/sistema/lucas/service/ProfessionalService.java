// backend/src/main/java/com/sistema/lucas/service/ProfessionalService.java
package com.sistema.lucas.service;

import com.sistema.lucas.model.Professional;
import com.sistema.lucas.model.dto.ProfessionalCreateDTO;
import com.sistema.lucas.model.dto.ProfessionalUpdateDTO;
import com.sistema.lucas.model.enums.Role;
import com.sistema.lucas.repository.ProfessionalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ProfessionalService {

    @Autowired private ProfessionalRepository repository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private com.sistema.lucas.repository.AppointmentRepository appointmentRepository;
    @Autowired private com.sistema.lucas.repository.ProfessionalAvailabilityRepository availabilityRepository;
    @Autowired private com.sistema.lucas.repository.ProntuarioRepository prontuarioRepository;
    @Autowired private com.sistema.lucas.repository.DocumentoRepository documentoRepository;
    @Autowired private com.sistema.lucas.repository.UserRepository userRepository;

    public List<Professional> findAll() {
        return repository.findAll();
    }

    @Transactional
    public void create(ProfessionalCreateDTO dto) {
        var email = dto.email().trim().toLowerCase();

        var existing = userRepository.findByEmail(email);
        if (existing != null) {
            if (existing.getRole() == Role.PATIENT) {
                throw new RuntimeException("Erro: Este e-mail já pertence a um paciente cadastrado. "
                    + "Use outro e-mail para o perfil profissional.");
            }
            throw new RuntimeException("Erro: E-mail já cadastrado no sistema.");
        }

        // ✅ era existsByCrm, agora verifica registroConselho (normalizado, igual ao @PrePersist)
        var registro = dto.registroConselho().trim().toUpperCase();
        if (repository.existsByRegistroConselho(registro)) {
            throw new RuntimeException("Erro: Este registro já está cadastrado no sistema.");
        }

        Professional professional = new Professional();
        professional.setName(dto.name());
        professional.setEmail(email);
        professional.setPassword(passwordEncoder.encode(dto.password()));
        professional.setRole(Role.PROFESSIONAL);
        professional.setTipoRegistro(dto.tipoRegistro()); // ✅ novo
        professional.setRegistroConselho(registro); // ✅ era crm
        professional.setSpecialty(dto.specialty());

        try {
            repository.save(professional);
            repository.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new RuntimeException("Erro: Este e-mail ou registro já está em uso por outra conta.");
        }
    }

    @Transactional
    public void update(@org.springframework.lang.NonNull Long id, ProfessionalCreateDTO dto) {
        Professional professional = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Profissional não encontrado"));

        var email = dto.email().trim().toLowerCase();
        if (!professional.getEmail().equals(email) && userRepository.findByEmail(email) != null) {
            throw new RuntimeException("Erro: E-mail já cadastrado no sistema.");
        }

        var registro = dto.registroConselho().trim().toUpperCase();
        if (!professional.getRegistroConselho().equals(registro)
                && repository.existsByRegistroConselho(registro)) {
            throw new RuntimeException("Erro: Este registro já está cadastrado no sistema.");
        }

        professional.setName(dto.name());
        professional.setEmail(email);
        professional.setTipoRegistro(dto.tipoRegistro());
        professional.setRegistroConselho(registro);
        professional.setSpecialty(dto.specialty());

        if (dto.password() != null && !dto.password().trim().isEmpty()) {
            professional.setPassword(passwordEncoder.encode(dto.password()));
        }

        repository.save(professional);
    }

    @Transactional
    public void updateMyProfile(String email, ProfessionalUpdateDTO dto) {
        Professional professional = repository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Profissional não encontrado"));

        if (dto.name() != null && !dto.name().trim().isEmpty()) {
            professional.setName(dto.name());
        }

        if (dto.email() != null && !dto.email().trim().isEmpty()) {
            var novoEmail = dto.email().trim().toLowerCase();
            if (!novoEmail.equals(professional.getEmail())) {
                if (userRepository.findByEmail(novoEmail) != null) {
                    throw new RuntimeException("Erro: E-mail já cadastrado por outro usuário.");
                }
                professional.setEmail(novoEmail);
            }
        }

        if (dto.tipoRegistro() != null)    professional.setTipoRegistro(dto.tipoRegistro());
        if (dto.registroConselho() != null) professional.setRegistroConselho(dto.registroConselho());
        if (dto.specialty() != null)        professional.setSpecialty(dto.specialty());

        if (dto.cpf() != null)              professional.setCpf(dto.cpf());
        if (dto.phone() != null)            professional.setPhone(dto.phone());
        if (dto.birthDate() != null)        professional.setBirthDate(dto.birthDate());
        if (dto.gender() != null)           professional.setGender(dto.gender());
        if (dto.address() != null)          professional.setAddress(dto.address());

        if (dto.modalidadeAtendimento() != null) professional.setModalidadeAtendimento(dto.modalidadeAtendimento());

        if (dto.newPassword() != null && !dto.newPassword().trim().isEmpty()) {
            professional.setPassword(passwordEncoder.encode(dto.newPassword()));
        }

        repository.save(java.util.Objects.requireNonNull(professional));
    }

    @Transactional
    public void delete(@org.springframework.lang.NonNull Long id) {
        try {
            repository.deleteById(id);
            repository.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new RuntimeException("Não é possível excluir o profissional pois existem registros (como consultas ou prontuários) vinculados a ele.");
        }
    }

    @Transactional
    public void forceDelete(@org.springframework.lang.NonNull Long id) {
        Professional prof = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Profissional não encontrado"));

        // Apagar Documentos
        documentoRepository.deleteAll(java.util.Objects.requireNonNull(documentoRepository.findByProfissionalEmailOrderByCriadoEmDesc(prof.getEmail())));
        
        // Apagar Prontuários
        prontuarioRepository.deleteAll(java.util.Objects.requireNonNull(prontuarioRepository.findByProfessionalEmailOrderByCriadoEmDesc(prof.getEmail())));

        // Apagar Disponibilidades
        availabilityRepository.deleteAll(java.util.Objects.requireNonNull(availabilityRepository.findByProfessionalId(id)));

        // Apagar Consultas
        appointmentRepository.deleteAll(java.util.Objects.requireNonNull(appointmentRepository.findByProfessionalId(id)));

        // Finalmente, apagar o Profissional
        repository.delete(prof);
    }

    public Professional getMyProfile(String email) {
        return repository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Profissional não encontrado"));
    }
}