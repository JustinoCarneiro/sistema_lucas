// backend/src/main/java/com/sistema/lucas/controller/AppointmentController.java
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

    // ✅ NOVO: agenda de hoje (usada pelo componente professional-appointments)
    @GetMapping("/professional/today")
    public ResponseEntity<List<AppointmentResponseDTO>> getTodayAppointments(Principal principal) {
        return ResponseEntity.ok(service.getTodayAppointments(principal.getName()));
    }

    @GetMapping("/me")
    public ResponseEntity<List<AppointmentResponseDTO>> getMyAppointments(Principal principal) {
        return ResponseEntity.ok(service.getMyAppointments(principal.getName()));
    }

    // ✅ NOVO: buscar consulta por ID (usada pelo MedicalRecordComponent)
    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        service.cancel(id);
        return ResponseEntity.noContent().build();
    }

    // ✅ NOVO: marcar falta do paciente
    @PatchMapping("/{id}/no-show")
    public ResponseEntity<String> markNoShow(@PathVariable Long id) {
        service.markNoShow(id);
        return ResponseEntity.ok("Paciente marcado como faltante.");
    }
}