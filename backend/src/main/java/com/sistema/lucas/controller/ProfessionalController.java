// backend/src/main/java/com/sistema/lucas/controller/ProfessionalController.java
package com.sistema.lucas.controller;

import com.sistema.lucas.model.dto.ProfessionalCreateDTO;
import com.sistema.lucas.model.dto.ProfessionalResponseDTO;
import com.sistema.lucas.model.dto.ProfessionalUpdateDTO;
import com.sistema.lucas.service.AuditLogService;
import com.sistema.lucas.service.ProfessionalService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/professionals")
public class ProfessionalController {

    @Autowired private ProfessionalService service;
    @Autowired private AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<List<ProfessionalResponseDTO>> getAll() {
        return ResponseEntity.ok(
            service.findAll().stream().map(ProfessionalResponseDTO::new).toList()
        );
    }

    // ✅ /me antes de /{id}
    @GetMapping("/me")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<ProfessionalResponseDTO> getMyProfile(Principal principal) {
        return ResponseEntity.ok(new ProfessionalResponseDTO(service.getMyProfile(principal.getName())));
    }

    // ✅ /me antes de /{id}
    @PutMapping("/me")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<String> updateMyProfile(
            @RequestBody ProfessionalUpdateDTO dto,
            Principal principal) {
        service.updateMyProfile(principal.getName(), dto);
        auditLogService.log(principal.getName(), "ATUALIZACAO", "Professional", null, "Atualizou seu próprio perfil");
        return ResponseEntity.ok("Perfil atualizado com sucesso!");
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> create(@RequestBody @Valid ProfessionalCreateDTO dto, Principal principal) {
        service.create(dto);
        auditLogService.log(principal.getName(), "CRIACAO", "Professional", null, "Cadastrou profissional: " + dto.email());
        return ResponseEntity.status(201).body("Profissional cadastrado com sucesso!");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> update(
            @PathVariable Long id,
            @RequestBody @Valid ProfessionalCreateDTO dto,
            Principal principal) {
        service.update(java.util.Objects.requireNonNull(id), dto);
        auditLogService.log(principal.getName(), "ATUALIZACAO", "Professional", id, "Atualizou profissional ID: " + id);
        return ResponseEntity.ok("Profissional atualizado com sucesso!");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
        auditLogService.log(principal.getName(), "EXCLUSAO", "Professional", id, "Excluiu profissional ID: " + id);
        service.delete(java.util.Objects.requireNonNull(id));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/force/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> forceDelete(@PathVariable Long id, Principal principal) {
        auditLogService.log(principal.getName(), "EXCLUSAO_CASCATA", "Professional", id, "Exclusão forçada (cascata) do profissional ID: " + id);
        service.forceDelete(java.util.Objects.requireNonNull(id));
        return ResponseEntity.noContent().build();
    }
}