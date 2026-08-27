// backend/src/main/java/com/sistema/lucas/controller/ProntuarioController.java
package com.sistema.lucas.controller;

import com.sistema.lucas.model.dto.ProntuarioCreateDTO;
import com.sistema.lucas.model.dto.ProntuarioResponseDTO;
import com.sistema.lucas.service.ProntuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/prontuarios")
public class ProntuarioController {

    @Autowired private ProntuarioService service;

    @GetMapping("/paciente/{patientId}")
    @PreAuthorize("hasAnyRole('PROFESSIONAL', 'ADMIN')")
    public ResponseEntity<List<ProntuarioResponseDTO>> getByPaciente(@PathVariable Long patientId, Principal principal) {
        return ResponseEntity.ok(
            service.getByPatientId(patientId, principal.getName())
                .stream().map(ProntuarioResponseDTO::new).toList()
        );
    }

    @PostMapping
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<ProntuarioResponseDTO> create(
            @RequestBody @Valid ProntuarioCreateDTO dto,
            Principal principal) {
        var saved = service.create(dto.appointmentId(), dto.notas(), principal.getName());
        return ResponseEntity.status(201).body(new ProntuarioResponseDTO(saved));
    }
}
