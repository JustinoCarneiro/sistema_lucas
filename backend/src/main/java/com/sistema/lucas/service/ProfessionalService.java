package com.sistema.lucas.service;

import com.sistema.lucas.model.Professional;
import com.sistema.lucas.model.ProfessionalCreateDTO;
import com.sistema.lucas.repository.ProfessionalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProfessionalService {

    @Autowired
    private ProfessionalRepository repository;

    public List<Professional> findAll() {
        return repository.findAll();
    }

    @Transactional
    public void create(ProfessionalCreateDTO dto) {
        if (repository.existsByCrm(dto.crm())) {
            throw new RuntimeException("Erro: Este CRM já está cadastrado no sistema.");
        }
        
        var professional = new Professional(dto);
        repository.save(professional);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }
}