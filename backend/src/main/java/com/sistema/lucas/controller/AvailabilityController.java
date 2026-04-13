// backend/src/main/java/com/sistema/lucas/controller/AvailabilityController.java
package com.sistema.lucas.controller;

import com.sistema.lucas.model.ProfessionalAvailability;
import com.sistema.lucas.model.Professional;
import com.sistema.lucas.model.dto.AvailabilityDTO;
import com.sistema.lucas.model.dto.SlotDTO;
import com.sistema.lucas.service.AvailabilityService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/disponibilidade")
public class AvailabilityController {

    @Autowired private AvailabilityService service;

    // Profissional — ver minha grade semanal
    @GetMapping("/minha")
    public ResponseEntity<List<ProfessionalAvailability>> minhaDisponibilidade(Principal principal) {
        return ResponseEntity.ok(service.getMinhaDisponibilidade(principal.getName()));
    }

    // Profissional — salvar/atualizar um dia
    @PostMapping
    public ResponseEntity<String> salvarDia(
            @RequestBody @Valid AvailabilityDTO dto,
            Principal principal) {
        service.salvarDia(principal.getName(), dto);
        return ResponseEntity.ok("Disponibilidade salva com sucesso!");
    }

    // Profissional — remover um dia
    @DeleteMapping("/{dayOfWeek}")
    public ResponseEntity<Void> removerDia(
            @PathVariable DayOfWeek dayOfWeek,
            Principal principal) {
        service.removerDia(principal.getName(), dayOfWeek);
        return ResponseEntity.noContent().build();
    }

    // Paciente — listar profissionais que possuem disponibilidade configurada
    @GetMapping("/profissionais-disponiveis")
    public ResponseEntity<List<Map<String, Object>>> profissionaisDisponiveis() {
        List<Professional> profs = service.getProfissionaisComDisponibilidade();
        var result = profs.stream().map(p -> Map.<String, Object>of(
            "id", p.getId(),
            "name", p.getName(),
            "specialty", p.getSpecialty() != null ? p.getSpecialty() : ""
        )).toList();
        return ResponseEntity.ok(result);
    }

    // Paciente — buscar dias da semana que o profissional atende
    @GetMapping("/{professionalId}/working-days")
    public ResponseEntity<List<DayOfWeek>> getWorkingDays(@PathVariable Long professionalId) {
        return ResponseEntity.ok(service.getWorkingDays(professionalId));
    }

    // Paciente — buscar slots disponíveis para um profissional em uma data
    @GetMapping("/{professionalId}/slots")
    public ResponseEntity<List<SlotDTO>> slotsDisponiveis(
            @PathVariable Long professionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return ResponseEntity.ok(service.getSlotsDisponiveis(professionalId, data));
    }
}
