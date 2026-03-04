package com.sistema.lucas.controller;

import com.sistema.lucas.domain.Professional;
import com.sistema.lucas.domain.enums.Role;
import com.sistema.lucas.dto.professional.ProfessionalCreateDTO;
import com.sistema.lucas.dto.professional.ProfessionalResponseDTO;
import com.sistema.lucas.repository.ProfessionalRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/professionals")
@RequiredArgsConstructor
public class ProfessionalController {

    private final ProfessionalRepository professionalRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping
    public ResponseEntity<ProfessionalResponseDTO> createProfessional(@RequestBody @Valid ProfessionalCreateDTO data) {
        Professional newProfessional = new Professional();
        newProfessional.setName(data.name());
        newProfessional.setEmail(data.email());
        newProfessional.setCrm(data.crm());
        newProfessional.setSpecialty(data.specialty());
        newProfessional.setPassword(passwordEncoder.encode(data.password()));
        newProfessional.setActive(true);
        newProfessional.setRole(Role.PROFESSIONAL);

        Professional saved = professionalRepository.save(newProfessional);
        return ResponseEntity.ok(new ProfessionalResponseDTO(saved));
    }

    @GetMapping
    public ResponseEntity<Page<ProfessionalResponseDTO>> listAll(Pageable pagination) {
        Page<ProfessionalResponseDTO> page = professionalRepository
                .findAll(pagination)
                .map(ProfessionalResponseDTO::new);
        return ResponseEntity.ok(page);
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<ProfessionalResponseDTO> update(@PathVariable Long id, @RequestBody @Valid ProfessionalCreateDTO dto) {
        Professional professional = professionalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Médico não encontrado"));

        professional.setName(dto.name());
        professional.setSpecialty(dto.specialty());
        professional.setCrm(dto.crm());

        Professional updated = professionalRepository.save(professional);
        return ResponseEntity.ok(new ProfessionalResponseDTO(updated));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!professionalRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        professionalRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<ProfessionalResponseDTO> getMyProfile(@org.springframework.security.core.annotation.AuthenticationPrincipal com.sistema.lucas.domain.User loggedUser) {
        Professional professional = professionalRepository.findById(loggedUser.getId())
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Médico não encontrado"));
                
        return ResponseEntity.ok(new ProfessionalResponseDTO(professional));
    }
}
