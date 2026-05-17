// backend/src/main/java/com/sistema/lucas/controller/ProntuarioController.java
package com.sistema.lucas.controller;

import com.sistema.lucas.model.Prontuario;
import com.sistema.lucas.service.ProntuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/prontuarios")
public class ProntuarioController {

    @Autowired private ProntuarioService service;

    @GetMapping("/paciente/{patientId}")
    @PreAuthorize("hasAnyRole('PROFESSIONAL', 'ADMIN')")
    public ResponseEntity<List<Prontuario>> getByPaciente(@PathVariable Long patientId, Principal principal) {
        return ResponseEntity.ok(service.getByPatientId(patientId, principal.getName()));
    }

    @PostMapping
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<Prontuario> create(
            @RequestBody Map<String, Object> body,
            Principal principal) {

        Long appointmentId = Long.valueOf(body.get("appointmentId").toString());
        String notas = body.get("notas").toString();

        Prontuario saved = service.create(java.util.Objects.requireNonNull(appointmentId), notas, principal.getName());
        return ResponseEntity.status(201).body(saved);
    }
}