// backend/src/main/java/com/sistema/lucas/controller/AppointmentController.java
package com.sistema.lucas.controller;

import com.sistema.lucas.model.dto.AppointmentCancelDTO;
import com.sistema.lucas.model.dto.AppointmentCreateDTO;
import com.sistema.lucas.model.dto.AppointmentRescheduleDTO;
import com.sistema.lucas.model.dto.AppointmentResponseDTO;
import com.sistema.lucas.service.AppointmentService;
import com.sistema.lucas.service.AuditLogService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.security.Principal;

@RestController
@RequestMapping("/consultas")
public class AppointmentController {

    @Autowired private AppointmentService service;
    @Autowired private AuditLogService auditLogService;

    // Apenas pacientes agendam
    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<String> agendar(@RequestBody @Valid AppointmentCreateDTO dto, Principal principal) {
        service.agendar(dto, principal.getName());
        return ResponseEntity.status(201).body("Consulta agendada com sucesso!");
    }

    // Admin — somente leitura
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AppointmentResponseDTO>> listarTodas(Principal principal) {
        auditLogService.log(principal.getName(), "VISUALIZACAO_LISTA", "Appointment", null, "Listou todas as consultas do sistema");
        return ResponseEntity.ok(service.findAll());
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('PATIENT') or hasRole('PROFESSIONAL')")
    public ResponseEntity<Void> cancelar(@PathVariable Long id, @RequestBody @Valid AppointmentCancelDTO dto, Principal principal) {
        service.cancelar(java.util.Objects.requireNonNull(id), principal.getName(), dto.justification());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reagendar")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<Void> reagendar(@PathVariable Long id, @RequestBody @Valid AppointmentRescheduleDTO dto, Principal principal) {
        service.reagendar(java.util.Objects.requireNonNull(id), principal.getName(), dto.newDateTime(), dto.justification());
        return ResponseEntity.noContent().build();
    }

    // Paciente — suas consultas
    @GetMapping("/minhas")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<AppointmentResponseDTO>> minhasConsultas(Principal principal) {
        return ResponseEntity.ok(service.buscarPorPaciente(principal.getName()));
    }

    // Profissional — agenda do dia
    @GetMapping("/profissional/hoje")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<List<AppointmentResponseDTO>> agendaDeHoje(Principal principal) {
        return ResponseEntity.ok(service.agendaDeHoje(principal.getName()));
    }

    // Profissional — agenda completa
    @GetMapping("/profissional/todas")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<List<AppointmentResponseDTO>> todasDoProf(Principal principal) {
        return ResponseEntity.ok(service.buscarPorProfissional(principal.getName()));
    }

    // Profissional — consultas com data passada e status ainda pendente
    @GetMapping("/profissional/atrasadas")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<List<AppointmentResponseDTO>> getAtrasadas(Principal principal) {
        return ResponseEntity.ok(service.findAtrasadasPorProfissional(principal.getName()));
    }

    // Prontuário — buscar consulta por ID
    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponseDTO> buscarPorId(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(service.buscarPorId(java.util.Objects.requireNonNull(id), principal.getName()));
    }

    // Profissional — marcar falta
    @PatchMapping("/{id}/falta")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<String> marcarFalta(@PathVariable Long id, Principal principal) {
        service.marcarFalta(java.util.Objects.requireNonNull(id), principal.getName());
        return ResponseEntity.ok("Paciente marcado como faltante.");
    }

    // Paciente confirma presença
    @PatchMapping("/{id}/confirmar-paciente")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<String> confirmarPaciente(@PathVariable Long id, Principal principal) {
        service.confirmarPaciente(java.util.Objects.requireNonNull(id), principal.getName());
        return ResponseEntity.ok("Presença confirmada com sucesso!");
    }

    // Profissional confirma presença
    @PatchMapping("/{id}/confirmar-profissional")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<String> confirmarProfissional(@PathVariable Long id, Principal principal) {
        service.confirmarProfissional(java.util.Objects.requireNonNull(id), principal.getName());
        return ResponseEntity.ok("Consulta totalmente confirmada!");
    }

    // Profissional aprova solicitação pendente
    @PatchMapping("/{id}/aprovar")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<String> aprovar(@PathVariable Long id, Principal principal) {
        service.aprovarAgendamento(java.util.Objects.requireNonNull(id), principal.getName());
        return ResponseEntity.ok("Agendamento aprovado e paciente notificado.");
    }

    // Profissional recusa solicitação pendente
    @PatchMapping("/{id}/recusar")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<String> recusar(
            @PathVariable Long id,
            @RequestBody(required = false) AppointmentCancelDTO dto,
            Principal principal) {
        String justificativa = dto != null ? dto.justification() : "Indisponibilidade de agenda.";
        service.recusarAgendamento(java.util.Objects.requireNonNull(id), principal.getName(), justificativa);
        return ResponseEntity.ok("Agendamento recusado com sucesso.");
    }
}