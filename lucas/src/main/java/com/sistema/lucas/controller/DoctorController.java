package com.sistema.lucas.controller;

import com.sistema.lucas.domain.Doctor;
import com.sistema.lucas.repository.DoctorRepository; // Ajuste o pacote se o seu Repository estiver em outro lugar
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder; // <--- O nosso segurança que criptografa a senha!

    // 1. O nosso DTO moderno e limpo (criado aqui mesmo para facilitar)
    public record DoctorRequestDTO(String name, String email, String password, String crm, String specialty) {}

    @PostMapping
    public ResponseEntity<Doctor> createDoctor(@RequestBody DoctorRequestDTO data) {
        
        // 2. Montamos o Médico manualmente, com segurança!
        Doctor newDoctor = new Doctor();
        newDoctor.setName(data.name());
        newDoctor.setEmail(data.email());
        newDoctor.setCrm(data.crm());
        newDoctor.setSpecialty(data.specialty());
        
        // 3. O SEGREDO: Criptografar a senha ANTES de salvar no banco!
        newDoctor.setPassword(passwordEncoder.encode(data.password()));
        
        // 4. Acordando o médico (Ativo = true) e definindo a permissão
        newDoctor.setActive(true);
        // newDoctor.setRole(UserRole.DOCTOR); // Descomente e ajuste se você tiver um Enum de Roles

        // 5. Salva no banco
        doctorRepository.save(newDoctor);
        return ResponseEntity.ok(newDoctor);
    }

    @GetMapping
    public ResponseEntity<Page<Doctor>> listAll(Pageable pagination) {
        var page = doctorRepository.findAll(pagination);
        return ResponseEntity.ok(page);
    }
}