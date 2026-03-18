// backend/src/main/java/com/sistema/lucas/controller/PatientController.java
package com.sistema.lucas.controller;

import com.sistema.lucas.model.dto.PatientCreateDTO;
import com.sistema.lucas.model.dto.PatientResponseDTO;
import com.sistema.lucas.model.dto.PatientUpdateDTO;
import com.sistema.lucas.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.security.Principal;

@RestController
@RequestMapping("/patients")
public class PatientController {

    @Autowired private PatientService service;

    @GetMapping
    public ResponseEntity<List<PatientResponseDTO>> getAll() {
        return ResponseEntity.ok(
            service.findAll().stream().map(PatientResponseDTO::new).toList()
        );
    }

    // ✅ /me antes de /{id}
    @GetMapping("/me")
    public ResponseEntity<PatientResponseDTO> getMyProfile(Principal principal) {
        return ResponseEntity.ok(new PatientResponseDTO(service.getMyProfile(principal.getName())));
    }

    @PutMapping("/me")
    public ResponseEntity<String> updateMyProfile(
            @RequestBody PatientUpdateDTO dto,
            Principal principal) {
        service.updateMyProfile(principal.getName(), dto);
        return ResponseEntity.ok("Perfil atualizado com sucesso!");
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyConta(Principal principal) {
        service.deleteByEmail(principal.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}