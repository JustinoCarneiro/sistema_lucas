package com.sistema.lucas.controller;

import com.sistema.lucas.model.*;
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
    public ResponseEntity<List<Appointment>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @PostMapping
    public ResponseEntity<String> schedule(@RequestBody @Valid AppointmentCreateDTO dto) {
        service.schedule(dto);
        return ResponseEntity.status(201).body("Consulta agendada com sucesso!");
    }

    @GetMapping("/professional/today")
    public ResponseEntity<List<Appointment>> getTodayAppointments(Principal principal) {
        List<Appointment> appointments = service.getTodayAppointments(principal.getName());
        return ResponseEntity.ok(appointments);
    }

    @GetMapping("/me")
    public ResponseEntity<List<Appointment>> getMyAppointments(Principal principal) {
        return ResponseEntity.ok(service.getMyAppointments(principal.getName()));
    }
}