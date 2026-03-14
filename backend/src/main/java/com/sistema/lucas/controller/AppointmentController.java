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
@RequestMapping("/consultas")
public class AppointmentController {

    @Autowired private AppointmentService service;

    // Apenas pacientes agendam
    @PostMapping
    public ResponseEntity<String> agendar(@RequestBody @Valid AppointmentCreateDTO dto, Principal principal) {
        service.agendar(dto, principal.getName());
        return ResponseEntity.status(201).body("Consulta agendada com sucesso!");
    }

    // Admin — somente leitura
    @GetMapping
    public ResponseEntity<List<AppointmentResponseDTO>> listarTodas() {
        return ResponseEntity.ok(service.findAll());
    }

    // Admin — cancelar
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        service.cancelar(id);
        return ResponseEntity.noContent().build();
    }

    // Paciente — suas consultas
    @GetMapping("/minhas")
    public ResponseEntity<List<AppointmentResponseDTO>> minhasConsultas(Principal principal) {
        return ResponseEntity.ok(service.buscarPorPaciente(principal.getName()));
    }

    // Profissional — agenda do dia
    @GetMapping("/profissional/hoje")
    public ResponseEntity<List<AppointmentResponseDTO>> agendaDeHoje(Principal principal) {
        return ResponseEntity.ok(service.agendaDeHoje(principal.getName()));
    }

    // Profissional — agenda completa
    @GetMapping("/profissional/todas")
    public ResponseEntity<List<AppointmentResponseDTO>> todasDoProf(Principal principal) {
        return ResponseEntity.ok(service.buscarPorProfissional(principal.getName()));
    }

    // Prontuário — buscar consulta por ID
    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    // Profissional — marcar falta
    @PatchMapping("/{id}/falta")
    public ResponseEntity<String> marcarFalta(@PathVariable Long id) {
        service.marcarFalta(id);
        return ResponseEntity.ok("Paciente marcado como faltante.");
    }

    // Paciente confirma presença
    @PatchMapping("/{id}/confirmar-paciente")
    public ResponseEntity<String> confirmarPaciente(@PathVariable Long id, Principal principal) {
        service.confirmarPaciente(id, principal.getName());
        return ResponseEntity.ok("Presença confirmada com sucesso!");
    }

    // Profissional confirma presença
    @PatchMapping("/{id}/confirmar-profissional")
    public ResponseEntity<String> confirmarProfissional(@PathVariable Long id, Principal principal) {
        service.confirmarProfissional(id, principal.getName());
        return ResponseEntity.ok("Consulta totalmente confirmada!");
    }
}