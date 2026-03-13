package com.sistema.lucas.service;

import com.sistema.lucas.model.Patient;
import com.sistema.lucas.model.PatientCreateDTO;
import com.sistema.lucas.model.enums.Role; // Importar a Role
import com.sistema.lucas.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder; // Importar o encoder
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PatientService {

    @Autowired
    private PatientRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder; // Necessário para a senha

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
        
        // MAPEAMENTO MANUAL: Sem usar o construtor customizado
        Patient patient = new Patient();
        patient.setName(dto.name());
        patient.setEmail(dto.email());
        patient.setPassword(passwordEncoder.encode(dto.password())); // Criptografando
        patient.setRole(Role.PATIENT);
        patient.setCpf(dto.cpf());
        
        repository.save(patient);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Patient getMyProfile(String email) {
        return repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));
    }
}