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
}