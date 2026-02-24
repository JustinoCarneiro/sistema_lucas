package com.sistema.lucas.controller;

import com.sistema.lucas.domain.Doctor;
import com.sistema.lucas.domain.enums.Role;
import com.sistema.lucas.dto.DoctorCreateDTO;
import com.sistema.lucas.dto.DoctorResponseDTO;
import com.sistema.lucas.repository.DoctorRepository;
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
@RequestMapping("/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping
    public ResponseEntity<DoctorResponseDTO> createDoctor(@RequestBody @Valid DoctorCreateDTO data) {
        Doctor newDoctor = new Doctor();
        newDoctor.setName(data.name());
        newDoctor.setEmail(data.email());
        newDoctor.setCrm(data.crm());
        newDoctor.setSpecialty(data.specialty());
        newDoctor.setPassword(passwordEncoder.encode(data.password()));
        newDoctor.setActive(true);
        newDoctor.setRole(Role.DOCTOR);

        Doctor saved = doctorRepository.save(newDoctor);
        return ResponseEntity.ok(new DoctorResponseDTO(saved));
    }

    @GetMapping
    public ResponseEntity<Page<DoctorResponseDTO>> listAll(Pageable pagination) {
        Page<DoctorResponseDTO> page = doctorRepository
                .findAll(pagination)
                .map(DoctorResponseDTO::new);
        return ResponseEntity.ok(page);
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<DoctorResponseDTO> update(@PathVariable Long id, @RequestBody @Valid DoctorCreateDTO dto) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Médico não encontrado"));

        doctor.setName(dto.name());
        doctor.setSpecialty(dto.specialty());
        doctor.setCrm(dto.crm());

        Doctor updated = doctorRepository.save(doctor);
        return ResponseEntity.ok(new DoctorResponseDTO(updated));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!doctorRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        doctorRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<DoctorResponseDTO> getMyProfile(@org.springframework.security.core.annotation.AuthenticationPrincipal com.sistema.lucas.domain.User loggedUser) {
        Doctor doctor = doctorRepository.findById(loggedUser.getId())
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Médico não encontrado"));
                
        return ResponseEntity.ok(new DoctorResponseDTO(doctor));
    }
}
