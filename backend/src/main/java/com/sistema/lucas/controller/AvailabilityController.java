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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/disponibilidade")
public class AvailabilityController {

    @Autowired private AvailabilityService service;

    // Profissional — ver minha grade do mês
    @GetMapping("/minha")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<List<ProfessionalAvailability>> minhaDisponibilidade(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes,
            Principal principal) {
        return ResponseEntity.ok(service.getMinhaDisponibilidade(principal.getName(), mes));
    }

    // Profissional — salvar/atualizar o mês
    @PostMapping("/mensal")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<String> salvarMes(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes,
            @RequestBody @Valid List<AvailabilityDTO> dtos,
            Principal principal) {
        service.salvarMes(principal.getName(), dtos, mes);
        return ResponseEntity.ok("Disponibilidade do mês salva com sucesso!");
    }

    // Status do mês para o Frontend
    @GetMapping("/status-mes")
    public ResponseEntity<Map<String, Object>> getStatusMes() {
        LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        YearMonth mesAtual = YearMonth.from(hoje);
        LocalDate fimDoMes = mesAtual.atEndOfMonth();
        long diasParaFim = ChronoUnit.DAYS.between(hoje, fimDoMes);
        
        return ResponseEntity.ok(Map.of(
            "diasRestantes", diasParaFim,
            "bloqueado", false // Não há mais bloqueio, apenas avisos.
        ));
    }

    // Paciente — listar profissionais que possuem disponibilidade configurada
    @GetMapping("/profissionais-disponiveis")
    public ResponseEntity<List<Map<String, Object>>> profissionaisDisponiveis() {
        List<Professional> profs = service.getProfissionaisComDisponibilidade();
        var result = profs.stream().map(p -> Map.<String, Object>of(
            "id", p.getId(),
            "name", p.getName(),
            "specialty", p.getSpecialty() != null ? p.getSpecialty() : "",
            "modalidadeAtendimento", p.getModalidadeAtendimento() != null
                ? p.getModalidadeAtendimento().name() : "PRESENCIAL"
        )).toList();
        return ResponseEntity.ok(result);
    }

    // Paciente — buscar dias da semana que o profissional atende
    @GetMapping("/{professionalId}/working-days")
    public ResponseEntity<List<DayOfWeek>> getWorkingDays(@PathVariable Long professionalId) {
        return ResponseEntity.ok(service.getWorkingDays(professionalId));
    }

    // Paciente — buscar datas reais com disponibilidade cadastrada
    @GetMapping("/{professionalId}/available-dates")
    public ResponseEntity<List<LocalDate>> getAvailableDates(@PathVariable Long professionalId) {
        return ResponseEntity.ok(service.getAvailableDates(professionalId));
    }

    // Paciente — buscar slots disponíveis para um profissional em uma data
    @GetMapping("/{professionalId}/slots")
    public ResponseEntity<List<SlotDTO>> slotsDisponiveis(
            @PathVariable Long professionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return ResponseEntity.ok(service.getSlotsDisponiveis(java.util.Objects.requireNonNull(professionalId), data));
    }
}
