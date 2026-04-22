package com.sistema.lucas.service;

import com.sistema.lucas.model.Patient;
import com.sistema.lucas.model.dto.PatientCreateDTO;
import com.sistema.lucas.model.enums.Role; // Importar a Role
import com.sistema.lucas.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder; // Importar o encoder
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sistema.lucas.model.dto.PatientUpdateDTO;

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
        try {
            repository.deleteById(id);
            repository.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new RuntimeException("Não é possível excluir o paciente pois existem registros (como consultas ou prontuários) vinculados a ele.");
        }
    }

    public Patient getMyProfile(String email) {
        return repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));
    }

    @Transactional
    public void deleteByEmail(String email) {
        Patient patient = repository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));
        try {
            repository.delete(patient);
            repository.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new RuntimeException("Não é possível excluir o paciente pois existem registros (como consultas ou prontuários) vinculados a ele.");
        }
    }

    // Adicionar ao PatientService.java
    @Transactional
    public void updateMyProfile(String email, PatientUpdateDTO dto) {
        Patient patient = repository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));

        if (dto.name() != null && !dto.name().trim().isEmpty()) {
            patient.setName(dto.name());
        }

        if (dto.email() != null && !dto.email().trim().isEmpty() && !dto.email().equals(patient.getEmail())) {
            if (repository.existsByEmail(dto.email())) {
                throw new RuntimeException("Erro: E-mail já cadastrado por outro usuário.");
            }
            patient.setEmail(dto.email());
        }

        if (dto.cpf() != null && !dto.cpf().trim().isEmpty() && !dto.cpf().equals(patient.getCpf())) {
            if (repository.existsByCpf(dto.cpf())) {
                throw new RuntimeException("Erro: CPF já cadastrado por outro usuário.");
            }
            patient.setCpf(dto.cpf());
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

        repository.save(patient);
    }
}