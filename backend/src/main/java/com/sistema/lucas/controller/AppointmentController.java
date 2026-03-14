package com.sistema.lucas.controller;

import com.sistema.lucas.model.dto.AppointmentCreateDTO;
import com.sistema.lucas.model.dto.AppointmentResponseDTO; // Import necessário
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

    @Autowired
    private AppointmentService service;

    @GetMapping
    public ResponseEntity<List<AppointmentResponseDTO>> getAll() {
        // Alterado para AppointmentResponseDTO para evitar erro 500 de recursão
        return ResponseEntity.ok(service.findAll());
    }

    @PostMapping
    public ResponseEntity<String> schedule(@RequestBody @Valid AppointmentCreateDTO dto) {
        service.schedule(dto);
        return ResponseEntity.status(201).body("Consulta agendada com sucesso!");
    }

    @GetMapping("/me")
    public ResponseEntity<List<AppointmentResponseDTO>> getMyAppointments(Principal principal) {
        // O principal.getName() contém o e-mail do utilizador logado extraído do JWT
        return ResponseEntity.ok(service.getMyAppointments(principal.getName()));
    }

    @GetMapping("/professional/today")
    public ResponseEntity<List<AppointmentResponseDTO>> getTodayAppointments(Principal principal) {
        return ResponseEntity.ok(service.getTodayAppointments(principal.getName()));
    }
}