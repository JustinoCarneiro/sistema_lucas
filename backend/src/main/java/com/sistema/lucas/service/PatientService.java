package com.sistema.lucas.service;

import com.sistema.lucas.model.Patient;
import com.sistema.lucas.model.PatientCreateDTO;
import com.sistema.lucas.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PatientService {

    @Autowired
    private PatientRepository repository;

    public List<Patient> findAll() {
        return repository.findAll();
    }

    @Transactional
    public void create(PatientCreateDTO dto) {
        if (repository.existsByCpf(dto.cpf())) {
            throw new RuntimeException("Erro: CPF já cadastrado.");
        }
        if (repository.existsByEmail(dto.email())) {
            throw new RuntimeException("Erro: Email já cadastrado.");
        }
        
        var patient = new Patient(dto);
        repository.save(patient);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }
}