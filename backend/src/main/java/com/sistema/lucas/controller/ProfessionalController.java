// backend/src/main/java/com/sistema/lucas/controller/ProfessionalController.java
package com.sistema.lucas.controller;

import com.sistema.lucas.model.Professional;
import com.sistema.lucas.model.dto.ProfessionalCreateDTO;
import com.sistema.lucas.model.dto.ProfessionalUpdateDTO;
import com.sistema.lucas.service.ProfessionalService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/professionals")
public class ProfessionalController {

    @Autowired private ProfessionalService service;

    @GetMapping
    public ResponseEntity<List<Professional>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    // ✅ /me antes de /{id}
    @GetMapping("/me")
    public ResponseEntity<Professional> getMyProfile(Principal principal) {
        return ResponseEntity.ok(service.getMyProfile(principal.getName()));
    }

    // ✅ /me antes de /{id}
    @PutMapping("/me")
    public ResponseEntity<String> updateMyProfile(
            @RequestBody ProfessionalUpdateDTO dto,
            Principal principal) {
        service.updateMyProfile(principal.getName(), dto);
        return ResponseEntity.ok("Perfil atualizado com sucesso!");
    }

    @PostMapping
    public ResponseEntity<String> create(@RequestBody @Valid ProfessionalCreateDTO dto) {
        service.create(dto);
        return ResponseEntity.status(201).body("Profissional cadastrado com sucesso!");
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> update(
            @PathVariable Long id,
            @RequestBody @Valid ProfessionalCreateDTO dto) {
        service.update(id, dto);
        return ResponseEntity.ok("Profissional atualizado com sucesso!");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}