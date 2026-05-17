// backend/src/main/java/com/sistema/lucas/controller/PatientController.java
package com.sistema.lucas.controller;


import com.sistema.lucas.model.dto.PatientResponseDTO;
import com.sistema.lucas.model.dto.PatientUpdateDTO;
import com.sistema.lucas.service.AuditLogService;
import com.sistema.lucas.service.PatientService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.security.Principal;

@RestController
@RequestMapping("/patients")
public class PatientController {

    @Autowired private PatientService service;
    @Autowired private AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSIONAL')")
    public ResponseEntity<List<PatientResponseDTO>> getAll(Principal principal) {
        auditLogService.log(principal.getName(), "VISUALIZACAO_LISTA", "Patient", null, "Listou todos os pacientes");
        return ResponseEntity.ok(
            service.findAll().stream().map(PatientResponseDTO::new).toList()
        );
    }

    // ✅ /me antes de /{id}
    @GetMapping("/me")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<PatientResponseDTO> getMyProfile(Principal principal) {
        return ResponseEntity.ok(new PatientResponseDTO(service.getMyProfile(principal.getName())));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<String> updateMyProfile(
            @RequestBody PatientUpdateDTO dto,
            Principal principal) {
        service.updateMyProfile(principal.getName(), dto);
        auditLogService.log(principal.getName(), "ATUALIZACAO", "Patient", null, "Atualizou seu próprio perfil");
        return ResponseEntity.ok("Perfil atualizado com sucesso!");
    }

    @DeleteMapping("/me")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<Void> deleteMyConta(Principal principal) {
        auditLogService.log(principal.getName(), "EXCLUSAO_CONTA", "Patient", null, "Solicitou exclusão da própria conta");
        service.deleteByEmail(principal.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
        auditLogService.log(principal.getName(), "EXCLUSAO", "Patient", id, "Admin excluiu paciente ID: " + id);
        service.delete(java.util.Objects.requireNonNull(id));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/desbloquear")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> desbloquear(@PathVariable Long id, Principal principal) {
        service.desbloquear(java.util.Objects.requireNonNull(id));
        auditLogService.log(principal.getName(), "DESBLOQUEIO", "Patient", id, "Admin desbloqueou agendamentos para o paciente ID: " + id);
        return ResponseEntity.ok("Paciente desbloqueado com sucesso!");
    }
}
