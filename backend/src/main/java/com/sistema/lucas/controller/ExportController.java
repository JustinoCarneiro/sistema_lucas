package com.sistema.lucas.controller;

import com.sistema.lucas.service.AuditLogService;
import com.sistema.lucas.service.ExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;
    private final AuditLogService auditLogService;

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> exportAdmin(Principal principal) {
        auditLogService.log(principal.getName(), "EXPORTACAO", "Sistema", null, "Exportou relatório gerencial (Admin)");
        String csv = exportService.exportAdminData();
        return createCsvResponse(csv, "relatorio_admin.csv");
    }

    @GetMapping("/professional")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<String> exportProfessional(Principal principal) {
        auditLogService.log(principal.getName(), "EXPORTACAO", "Prontuarios", null, "Exportou seus atendimentos/prontuários");
        String csv = exportService.exportProfessionalData(principal.getName());
        return createCsvResponse(csv, "meus_atendimentos.csv");
    }

    @GetMapping("/patient")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<String> exportPatient(Principal principal) {
        auditLogService.log(principal.getName(), "EXPORTACAO", "Portabilidade", null, "Exportou seus próprios dados clínicos (Portabilidade)");
        String csv = exportService.exportPatientData(principal.getName());
        return createCsvResponse(csv, "meus_dados_clinicos.csv");
    }

    @GetMapping("/patients")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> exportPatients(Principal principal) {
        auditLogService.log(principal.getName(), "EXPORTACAO", "Pacientes", null, "Exportou lista completa de pacientes");
        String csv = exportService.exportPatientsData();
        return createCsvResponse(csv, "lista_pacientes.csv");
    }

    @GetMapping("/professionals")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> exportProfessionals(Principal principal) {
        auditLogService.log(principal.getName(), "EXPORTACAO", "Profissionais", null, "Exportou lista completa de profissionais");
        String csv = exportService.exportProfessionalsData();
        return createCsvResponse(csv, "lista_profissionais.csv");
    }

    private ResponseEntity<String> createCsvResponse(String content, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(content);
    }
}
