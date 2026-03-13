package com.sistema.lucas.service;

import com.sistema.lucas.model.Professional;
import com.sistema.lucas.model.ProfessionalCreateDTO;
import com.sistema.lucas.model.enums.Role;
import com.sistema.lucas.repository.ProfessionalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProfessionalService {

    @Autowired
    private ProfessionalRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<Professional> findAll() {
        return repository.findAll();
    }

    @Transactional
    public void create(ProfessionalCreateDTO dto) {
        if (repository.existsByCrm(dto.crm())) {
            throw new RuntimeException("Erro: Este CRM já está cadastrado no sistema.");
        }
        
        // MAPEAMENTO MANUAL: Preenchendo campo a campo
        Professional professional = new Professional();
        professional.setName(dto.name());
        professional.setEmail(dto.email());
        professional.setPassword(passwordEncoder.encode(dto.password()));
        professional.setRole(Role.PROFESSIONAL);
        professional.setCrm(dto.crm());
        professional.setSpecialty(dto.specialty());
        
        repository.save(professional);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Transactional
    public void update(Long id, ProfessionalCreateDTO dto) {
        // 1. Busca o médico pelo ID
        Professional professional = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Erro: Profissional não encontrado."));

        // 2. Verifica se ele está a tentar mudar para um CRM que já pertence a outro médico
        if (!professional.getCrm().equals(dto.crm()) && repository.existsByCrm(dto.crm())) {
            throw new RuntimeException("Erro: Este CRM já está cadastrado no sistema.");
        }

        // 3. Atualiza os dados básicos
        professional.setName(dto.name());
        professional.setEmail(dto.email());
        professional.setCrm(dto.crm());
        professional.setSpecialty(dto.specialty());

        // 4. Só atualiza a senha se o Angular enviar uma nova senha preenchida
        if (dto.password() != null && !dto.password().trim().isEmpty()) {
            professional.setPassword(passwordEncoder.encode(dto.password()));
        }

        // O Hibernate salva automaticamente no fim da transação, mas podemos chamar o save para garantir
        repository.save(professional);
    }

    public Professional getMyProfile(String email) {
        return repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Profissional não encontrado"));
    }
}