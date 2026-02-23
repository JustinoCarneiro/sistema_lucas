package com.sistema.lucas.controller;

import com.sistema.lucas.domain.Patient;
import com.sistema.lucas.domain.enums.Role;
import com.sistema.lucas.dto.PatientCreateDTO;
import com.sistema.lucas.repository.PatientRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
public class PatientController {

    // Precisamos criar este Repository rapidinho, igual fizemos pro Doctor
    private final PatientRepository repository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping
    @Transactional
    public ResponseEntity<Patient> create(@RequestBody @Valid PatientCreateDTO dto) {
        var patient = new Patient();
        
        // Dados do Usuário
        patient.setName(dto.name());
        patient.setEmail(dto.email());
        patient.setPassword(passwordEncoder.encode(dto.password())); // <--- Criptografa!
        patient.setRole(Role.PATIENT);
        patient.setActive(true);
        
        // Dados do Paciente
        patient.setCpf(dto.cpf());
        patient.setHealthInsurance(dto.healthInsurance());

        repository.save(patient);

        return ResponseEntity.status(HttpStatus.CREATED).body(patient);
    }

    @GetMapping
    public ResponseEntity<Page<Patient>> listAll(Pageable pagination) {
        var page = repository.findAll(pagination);
        return ResponseEntity.ok(page);
    }

    // 1. Carrega os dados do próprio paciente logado
    @GetMapping("/me")
    public ResponseEntity<Patient> getMyProfile(@org.springframework.security.core.annotation.AuthenticationPrincipal com.sistema.lucas.domain.User loggedUser) {
        return repository.findById(loggedUser.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 2. Atualiza dados sensíveis (WhatsApp, Plano de Saúde e Senha)
    @PutMapping("/me")
    @Transactional
    public ResponseEntity<Void> updateMyProfile(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.sistema.lucas.domain.User loggedUser,
            @RequestBody java.util.Map<String, String> updates) {
        
        var patient = repository.findById(loggedUser.getId())
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));

        // Atualiza WhatsApp se enviado
        if (updates.containsKey("whatsapp")) patient.setWhatsapp(updates.get("whatsapp"));
        
        // Atualiza Plano de Saúde se enviado
        if (updates.containsKey("healthInsurance")) patient.setHealthInsurance(updates.get("healthInsurance"));

        // Atualiza Senha apenas se enviada e não estiver vazia
        if (updates.containsKey("newPassword") && !updates.get("newPassword").isBlank()) {
            patient.setPassword(passwordEncoder.encode(updates.get("newPassword")));
        }

        repository.save(patient);
        return ResponseEntity.noContent().build();
    }
}