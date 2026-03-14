package com.sistema.lucas.controller;

import com.sistema.lucas.model.dto.AppointmentCreateDTO;
import com.sistema.lucas.model.dto.AppointmentResponseDTO;
import com.sistema.lucas.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.security.Principal;

@RestController
@RequestMapping("/appointments")
public class AppointmentController {

    @Autowired private AppointmentService service;

    @PostMapping
    public ResponseEntity<String> schedule(@RequestBody @Valid AppointmentCreateDTO dto, Principal principal) {
        service.schedule(dto, principal.getName());
        return ResponseEntity.status(201).body("Consulta agendada com sucesso!");
    }

    @GetMapping("/professional/me")
    public ResponseEntity<List<AppointmentResponseDTO>> getMyProfessionalAppointments(Principal principal) {
        return ResponseEntity.ok(service.getMyProfessionalAppointments(principal.getName()));
    }

    @GetMapping("/me")
    public ResponseEntity<List<AppointmentResponseDTO>> getMyAppointments(Principal principal) {
        return ResponseEntity.ok(service.getMyAppointments(principal.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        service.cancel(id);
        return ResponseEntity.noContent().build();
    }
}