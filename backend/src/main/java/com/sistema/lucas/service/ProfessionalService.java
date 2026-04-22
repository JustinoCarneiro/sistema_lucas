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

    public List<Professional> findAll() {
        return repository.findAll();
    }

    @Transactional
    public void create(ProfessionalCreateDTO dto) {
        // ✅ era existsByCrm, agora verifica registroConselho
        if (repository.existsByRegistroConselho(dto.registroConselho())) {
            throw new RuntimeException("Erro: Este registro já está cadastrado no sistema.");
        }

        Professional professional = new Professional();
        professional.setName(dto.name());
        professional.setEmail(dto.email());
        professional.setPassword(passwordEncoder.encode(dto.password()));
        professional.setRole(Role.PROFESSIONAL);
        professional.setTipoRegistro(dto.tipoRegistro()); // ✅ novo
        professional.setRegistroConselho(dto.registroConselho()); // ✅ era crm
        professional.setSpecialty(dto.specialty());

        repository.save(professional);
    }

    @Transactional
    public void update(Long id, ProfessionalCreateDTO dto) {
        Professional professional = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Profissional não encontrado"));

        if (!professional.getRegistroConselho().equals(dto.registroConselho())
                && repository.existsByRegistroConselho(dto.registroConselho())) {
            throw new RuntimeException("Erro: Este registro já está cadastrado no sistema.");
        }

        professional.setName(dto.name());
        professional.setEmail(dto.email());
        professional.setTipoRegistro(dto.tipoRegistro());
        professional.setRegistroConselho(dto.registroConselho());
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

        if (dto.email() != null && !dto.email().trim().isEmpty() && !dto.email().equals(professional.getEmail())) {
            // Wait, ProfessionalRepository extends JpaRepository<Professional, Long>
            // We can check if email exists. Professional shares the `users` table via JOIN, but the method existsByEmail?
            // Wait! Does ProfessionalRepository have existsByEmail? Let's assume it does since we use extends UserRepository or similar, or we can use findByEmail.
            if (repository.findByEmail(dto.email()).isPresent()) {
                throw new RuntimeException("Erro: E-mail já cadastrado por outro usuário.");
            }
            professional.setEmail(dto.email());
        }

        if (dto.tipoRegistro() != null)    professional.setTipoRegistro(dto.tipoRegistro());
        if (dto.registroConselho() != null) professional.setRegistroConselho(dto.registroConselho());
        if (dto.specialty() != null)        professional.setSpecialty(dto.specialty());

        if (dto.cpf() != null)              professional.setCpf(dto.cpf());
        if (dto.phone() != null)            professional.setPhone(dto.phone());
        if (dto.birthDate() != null)        professional.setBirthDate(dto.birthDate());
        if (dto.gender() != null)           professional.setGender(dto.gender());
        if (dto.address() != null)          professional.setAddress(dto.address());

        if (dto.newPassword() != null && !dto.newPassword().trim().isEmpty()) {
            professional.setPassword(passwordEncoder.encode(dto.newPassword()));
        }

        repository.save(professional);
    }

    @Transactional
    public void delete(Long id) {
        try {
            repository.deleteById(id);
            repository.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new RuntimeException("Não é possível excluir o profissional pois existem registros (como consultas ou prontuários) vinculados a ele.");
        }
    }

    @Transactional
    public void forceDelete(Long id) {
        Professional prof = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Profissional não encontrado"));

        // Apagar Documentos
        documentoRepository.deleteAll(documentoRepository.findByProfissionalEmailOrderByCriadoEmDesc(prof.getEmail()));
        
        // Apagar Prontuários
        prontuarioRepository.deleteAll(prontuarioRepository.findByProfessionalEmailOrderByCriadoEmDesc(prof.getEmail()));

        // Apagar Disponibilidades
        availabilityRepository.deleteAll(availabilityRepository.findByProfessionalId(id));

        // Apagar Consultas
        appointmentRepository.deleteAll(appointmentRepository.findByProfessionalId(id));

        // Finalmente, apagar o Profissional
        repository.delete(prof);
    }

    public Professional getMyProfile(String email) {
        return repository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Profissional não encontrado"));
    }
}