// backend/src/main/java/com/sistema/lucas/controller/DashboardController.java
package com.sistema.lucas.controller;

import com.sistema.lucas.model.enums.StatusConsulta;
import com.sistema.lucas.repository.*;
import com.sistema.lucas.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.sistema.lucas.model.Appointment;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private ProfessionalRepository professionalRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private ProntuarioRepository prontuarioRepository;
    @Autowired private DocumentoRepository documentoRepository;
    @Autowired private AuditLogService auditLogService;

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> dashboardAdmin(Principal principal) {
        auditLogService.log(principal.getName(), "VISUALIZACAO", "Dashboard", null, "Acessou dashboard administrativo");
        Map<String, Object> dados = new LinkedHashMap<>();

        dados.put("totalProfissionais", professionalRepository.count());
        dados.put("totalPacientes", patientRepository.count());

        // Consultas de hoje
        LocalDateTime inicioDia = LocalDate.now().atStartOfDay();
        LocalDateTime fimDia = inicioDia.plusDays(1);
        List<Appointment> consultasHoje = appointmentRepository.countByDateRange(inicioDia, fimDia);
        dados.put("consultasHoje", consultasHoje.size());

        // Consultas por status
        Map<String, Long> porStatus = new LinkedHashMap<>();
        porStatus.put("AGENDADA",  appointmentRepository.countByStatus(StatusConsulta.AGENDADA));
        porStatus.put("CONCLUIDA", appointmentRepository.countByStatus(StatusConsulta.CONCLUIDA));
        porStatus.put("CANCELADA", appointmentRepository.countByStatus(StatusConsulta.CANCELADA));
        porStatus.put("FALTA",     appointmentRepository.countByStatus(StatusConsulta.FALTA));
        dados.put("consultasPorStatus", porStatus);

        return ResponseEntity.ok(dados);
    }

    @GetMapping("/profissional")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<Map<String, Object>> dashboardProfissional(Principal principal) {
        String email = principal.getName();
        auditLogService.log(email, "VISUALIZACAO", "Dashboard", null, "Acessou dashboard profissional");
        Map<String, Object> dados = new LinkedHashMap<>();

        // Consultas de hoje
        dados.put("consultasHoje",
            appointmentRepository.findTodayAppointmentsByProfessionalEmail(email));

        // Consultas pendentes de confirmação pelo profissional
        dados.put("pendentesConfirmacao",
            appointmentRepository.countByProfessionalEmailAndStatus(email, StatusConsulta.AGUARDANDO_CONFIRMACAO));

        // Consultas com data passada e status ainda pendente (requer ação urgente)
        dados.put("consultasAtrasadas",
            appointmentRepository.countAtrasadasByProfessionalEmail(
                email, LocalDateTime.now(),
                List.of(StatusConsulta.AGUARDANDO_CONFIRMACAO, StatusConsulta.AGENDADA,
                        StatusConsulta.CONFIRMADA_PROFISSIONAL, StatusConsulta.CONFIRMADA)));

        // Próximas consultas agendadas
        dados.put("proximasConsultas",
            appointmentRepository.findProximasByProfissionalEmail(email, LocalDateTime.now()));

        // Total de pacientes únicos atendidos
        dados.put("totalPacientes",
            appointmentRepository.countPacientesUnicosByProfissional(email));

        // Últimos prontuários
        dados.put("ultimosProntuarios",
            prontuarioRepository.findByProfessionalEmailOrderByCriadoEmDesc(email)
                .stream().limit(5).toList());

        // Documentos recentes
        dados.put("documentosRecentes",
            documentoRepository.findByProfissionalEmailOrderByCriadoEmDesc(email)
                .stream().limit(5).toList());

        return ResponseEntity.ok(dados);
    }

    @GetMapping("/paciente")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<Map<String, Object>> dashboardPaciente(Principal principal) {
        String email = principal.getName();
        auditLogService.log(email, "VISUALIZACAO", "Dashboard", null, "Acessou dashboard paciente");
        Map<String, Object> dados = new LinkedHashMap<>();

        // Próxima consulta agendada
        dados.put("proximaConsulta",
            appointmentRepository.findProximaByPacienteEmail(email, LocalDateTime.now()));

        // Consultas pendentes de confirmação de presença pelo paciente
        dados.put("pendentesConfirmacao",
            appointmentRepository.countByPatientEmailAndStatus(email, StatusConsulta.CONFIRMADA_PROFISSIONAL));

        // Total de consultas realizadas
        dados.put("totalRealizadas",
            appointmentRepository.countByPatientEmailAndStatus(email, StatusConsulta.CONCLUIDA));

        // Total de consultas agendadas
        dados.put("totalAgendadas",
            appointmentRepository.countByPatientEmailAndStatus(email, StatusConsulta.AGENDADA));

        // Documentos disponíveis
        dados.put("documentosDisponiveis",
            documentoRepository.findByPacienteEmailAndDisponivelTrueOrderByCriadoEmDesc(email)
                .stream().limit(5).toList());

        // Perfil resumido
        var paciente = patientRepository.findByEmail(email);
        paciente.ifPresent(p -> {
            Map<String, String> perfil = new LinkedHashMap<>();
            perfil.put("nome", p.getName());
            perfil.put("email", p.getEmail());
            perfil.put("telefone", p.getPhone() != null ? p.getPhone() : "—");
            dados.put("perfil", perfil);
        });

        return ResponseEntity.ok(dados);
    }
}