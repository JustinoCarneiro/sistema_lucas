// backend/src/main/java/com/sistema/lucas/service/AppointmentService.java
package com.sistema.lucas.service;

import com.sistema.lucas.model.*;
import com.sistema.lucas.model.dto.*;
import com.sistema.lucas.model.enums.StatusConsulta;
import com.sistema.lucas.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class AppointmentService {

    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private ProfessionalRepository professionalRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EmailTemplateService emailTemplateService;
    @Autowired private ProfessionalAvailabilityRepository availabilityRepository;
    @Autowired private AuditLogService auditLogService;

    public static final List<StatusConsulta> STATUSES_PENDENTES = List.of(
        StatusConsulta.AGUARDANDO_CONFIRMACAO,
        StatusConsulta.AGENDADA,
        StatusConsulta.CONFIRMADA_PROFISSIONAL,
        StatusConsulta.CONFIRMADA);

    // ─── Leitura ─────────────────────────────────────────────────────────────

    public List<AppointmentResponseDTO> findAll() {
        return appointmentRepository.findAll().stream().map(AppointmentResponseDTO::new).toList();
    }

    public List<AppointmentResponseDTO> findAtrasadasPorProfissional(String email) {
        return appointmentRepository
            .findAtrasadasByProfessionalEmail(email, LocalDateTime.now(), STATUSES_PENDENTES)
            .stream().map(AppointmentResponseDTO::new).toList();
    }

    public List<AppointmentResponseDTO> buscarPorPaciente(String email) {
        return appointmentRepository.findByPatientEmail(email).stream().map(AppointmentResponseDTO::new).toList();
    }

    public List<AppointmentResponseDTO> buscarPorProfissional(String email) {
        return appointmentRepository.findByProfessionalEmail(email).stream().map(AppointmentResponseDTO::new).toList();
    }

    public List<AppointmentResponseDTO> agendaDeHoje(String email) {
        return appointmentRepository.findTodayAppointmentsByProfessionalEmail(email)
            .stream().map(AppointmentResponseDTO::new).toList();
    }

    public AppointmentResponseDTO buscarPorId(@org.springframework.lang.NonNull Long id, String email) {
        var consulta = appointmentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Consulta não encontrada"));
            
        // 🛡️ Segurança (IDOR): Apenas paciente logado ou o médico logado podem ver isso
        if (!consulta.getPatient().getEmail().equals(email) && !consulta.getProfessional().getEmail().equals(email)) {
            throw new RuntimeException("Operação de Segurança: Acesso negado (IDOR). Você não pode vasculhar informações de terceiros.");
        }
        return new AppointmentResponseDTO(consulta);
    }

    // ─── Agendamento ─────────────────────────────────────────────────────────

    // Apenas pacientes agendam — via token JWT
    @Transactional
    public void agendar(AppointmentCreateDTO dto, String emailPaciente) {
        var profissional = professionalRepository.findById(java.util.Objects.requireNonNull(dto.professionalId()))
            .orElseThrow(() -> new RuntimeException("Profissional não encontrado"));
        var paciente = patientRepository.findByEmail(emailPaciente)
            .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));

        if (paciente.getBlockedUntil() != null && paciente.getBlockedUntil().isAfter(java.time.LocalDateTime.now())) {
            String data = paciente.getBlockedUntil().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            throw new RuntimeException("Você está temporariamente bloqueado para novos agendamentos até " + data + " devido a um cancelamento/reagendamento tardio.");
        }

        // ✅ Validar que o profissional atende nesse dia
        var dataConsulta = dto.dateTime().toLocalDate();
        var availability = availabilityRepository
            .findByProfessionalEmailAndDate(profissional.getEmail(), dataConsulta);
        
        // ✅ Validar que o slot existe na disponibilidade do profissional
        LocalTime horario = dto.dateTime().toLocalTime().withMinute(0).withSecond(0).withNano(0);
        
        boolean slotDisponivel = availability.stream()
            .anyMatch(a -> a.getStartTime().equals(horario));
        
        if (!slotDisponivel) {
            throw new RuntimeException("O profissional não atende neste horário ou dia da semana.");
        }

        // ✅ Validar conflito de horário (já existe consulta nesse horário?)
        LocalDateTime inicioDia = dto.dateTime().toLocalDate().atStartOfDay();
        LocalDateTime fimDia = dto.dateTime().toLocalDate().plusDays(1).atStartOfDay();
        var consultasExistentes = appointmentRepository
            .findByProfessionalIdAndDateTimeBetweenAndStatusNot(
                dto.professionalId(), inicioDia, fimDia, StatusConsulta.CANCELADA
            );

        boolean conflito = consultasExistentes.stream()
            .anyMatch(c -> c.getDateTime().toLocalTime().equals(horario));

        if (conflito) {
            throw new RuntimeException("Este horário já está ocupado. Escolha outro horário.");
        }

        var consulta = new Appointment(profissional, paciente, dto);
        appointmentRepository.save(consulta);
        emailTemplateService.notificarPacienteAgendamentoPendente(consulta);
        emailTemplateService.notificarSolicitacaoAgendamentoParaMedico(consulta);
    }

    // ─── Cancelamento ────────────────────────────────────────────────────────

    @Transactional
    public void cancelar(@org.springframework.lang.NonNull Long id, String email, String justificativa) {
        var consulta = appointmentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Consulta não encontrada"));
            
        // 🛡️ Segurança (IDOR) - Permitir Paciente, Profissional ou ADMIN
        var usuarioAcao = userRepository.findByEmail(email);
        boolean isOwner = consulta.getPatient().getEmail().equals(email) || consulta.getProfessional().getEmail().equals(email);
        boolean isAdmin = usuarioAcao != null && usuarioAcao.getRole() == com.sistema.lucas.model.enums.Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new RuntimeException("Operação de Segurança: Tentativa de cancelamento malicioso bloqueada.");
        }

        // ✅ Regra de 24h: Aplica penalidade mas permite a ação
        aplicarPenalidadeSeNecessario(consulta);

        // ✅ Justificativa Obrigatória
        if (justificativa == null || justificativa.isBlank()) {
            throw new RuntimeException("A justificativa é obrigatória para o cancelamento.");
        }
        
        consulta.setStatus(StatusConsulta.CANCELADA);
        consulta.setCancelReason(justificativa);
        appointmentRepository.save(consulta);
        
        auditLogService.log(email, "CANCELAMENTO_CONSULTA", "Appointment", id, "Justificativa: " + justificativa);
        emailTemplateService.notificarConsultaCancelada(consulta); // ✅ e-mail
    }

    @Transactional
    public void reagendar(@org.springframework.lang.NonNull Long id, String email, LocalDateTime novaData, String justificativa) {
        var consulta = appointmentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Consulta não encontrada"));

        // 🛡️ Segurança (IDOR)
        if (!consulta.getPatient().getEmail().equals(email)) {
            throw new RuntimeException("Apenas o paciente pode reagendar sua própria consulta.");
        }

        // ✅ Regra de 24h (validar a consulta original): Aplica penalidade mas permite
        aplicarPenalidadeSeNecessario(consulta);

        // ✅ Justificativa Obrigatória
        if (justificativa == null || justificativa.isBlank()) {
            throw new RuntimeException("A justificativa é obrigatória para o reagendamento.");
        }

        // ✅ Validar disponibilidade na nova data
        var profissional = consulta.getProfessional();
        var dataConsulta = novaData.toLocalDate();
        var availability = availabilityRepository
            .findByProfessionalEmailAndDate(profissional.getEmail(), dataConsulta);
        
        LocalTime horario = novaData.toLocalTime().withMinute(0).withSecond(0).withNano(0);
        boolean slotDisponivel = availability.stream()
            .anyMatch(a -> a.getStartTime().equals(horario));
        
        if (!slotDisponivel) {
            throw new RuntimeException("O profissional não atende neste novo horário ou dia da semana.");
        }

        // ✅ Validar conflito de horário
        LocalDateTime inicioDia = novaData.toLocalDate().atStartOfDay();
        LocalDateTime fimDia = novaData.toLocalDate().plusDays(1).atStartOfDay();
        var consultasExistentes = appointmentRepository
            .findByProfessionalIdAndDateTimeBetweenAndStatusNot(
                profissional.getId(), inicioDia, fimDia, StatusConsulta.CANCELADA
            );

        boolean conflito = consultasExistentes.stream()
            .anyMatch(c -> c.getDateTime().toLocalTime().equals(horario));

        if (conflito) {
            throw new RuntimeException("Este novo horário já está ocupado.");
        }

        consulta.setDateTime(novaData);
        consulta.setCancelReason(justificativa); // Reutilizando campo para justificativa de mudança
        consulta.setStatus(StatusConsulta.AGENDADA); // Volta para agendada (precisa reconfirmar?)
        appointmentRepository.save(consulta);

        auditLogService.log(email, "REAGENDAMENTO_CONSULTA", "Appointment", id, "Nova Data: " + novaData + " | Justificativa: " + justificativa);
        emailTemplateService.notificarConsultaAgendada(consulta);
    }

    // ─── Falta ───────────────────────────────────────────────────────────────

    @Transactional
    public void marcarFalta(@org.springframework.lang.NonNull Long id, String emailProfissional) {
        var consulta = appointmentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Consulta não encontrada"));

        if (!consulta.getProfessional().getEmail().equals(emailProfissional)) {
            throw new RuntimeException("Operação de Segurança: Você não é o médico dessa consulta.");
        }

        consulta.setStatus(StatusConsulta.FALTA);
        appointmentRepository.save(consulta);

        processarInfracaoPaciente(consulta.getPatient(), consulta);
    }

    // ─── Confirmação (Profissional primeiro, Paciente depois) ────────────────

    // Consultas pendentes de aprovação não geram penalidade (profissional ainda não aceitou).
    private void aplicarPenalidadeSeNecessario(Appointment consulta) {
        if (consulta.getStatus() == StatusConsulta.AGUARDANDO_CONFIRMACAO) {
            return;
        }
        if (java.time.LocalDateTime.now().isAfter(consulta.getDateTime().minusHours(24))) {
            processarInfracaoPaciente(consulta.getPatient(), consulta);
        }
    }

    private void processarInfracaoPaciente(com.sistema.lucas.model.Patient paciente, Appointment consulta) {
        paciente.setInfractionCount(paciente.getInfractionCount() + 1);

        if (!paciente.isReceivedFirstWarning()) {
            paciente.setReceivedFirstWarning(true);
            patientRepository.save(paciente);
            emailTemplateService.enviarAvisoPrimeiraFalta(paciente, consulta);
            auditLogService.log("SYSTEM", "ADVERTENCIA_PACIENTE", "Patient", paciente.getId(), "Advertência de 1ª Falta aplicada.");
        } else {
            java.time.LocalDateTime dataBloqueio = java.time.LocalDateTime.now().plusDays(15);
            paciente.setBlockedUntil(dataBloqueio);
            patientRepository.save(paciente);
            emailTemplateService.enviarAvisoBloqueioFalta(paciente, consulta, dataBloqueio);
            auditLogService.log("SYSTEM", "BLOQUEIO_FALTAS", "Patient", paciente.getId(), "Bloqueio automático de 15 dias aplicado.");
        }
    }

    @Transactional
    public void aprovarAgendamento(@org.springframework.lang.NonNull Long id, String emailProfissional) {
        var consulta = appointmentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Consulta não encontrada"));

        if (!consulta.getProfessional().getEmail().equals(emailProfissional)) {
            throw new RuntimeException("Operação não autorizada. Você não é o médico dessa consulta.");
        }

        if (consulta.getStatus() != StatusConsulta.AGUARDANDO_CONFIRMACAO) {
            throw new RuntimeException("Apenas consultas aguardando confirmação podem ser aprovadas.");
        }

        consulta.setStatus(StatusConsulta.AGENDADA);
        appointmentRepository.save(consulta);

        auditLogService.log(emailProfissional, "APROVACAO_AGENDAMENTO", "Appointment", id,
            "Aprovou consulta do paciente " + consulta.getPatient().getName());
        emailTemplateService.notificarPacienteAgendamentoAceito(consulta);
    }

    @Transactional
    public void recusarAgendamento(@org.springframework.lang.NonNull Long id, String emailProfissional, String justificativa) {
        var consulta = appointmentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Consulta não encontrada"));

        if (!consulta.getProfessional().getEmail().equals(emailProfissional)) {
            throw new RuntimeException("Operação não autorizada. Você não é o médico dessa consulta.");
        }

        if (consulta.getStatus() != StatusConsulta.AGUARDANDO_CONFIRMACAO) {
            throw new RuntimeException("Apenas consultas aguardando confirmação podem ser recusadas.");
        }

        if (justificativa == null || justificativa.isBlank()) {
            throw new RuntimeException("A justificativa é obrigatória para recusar a consulta.");
        }

        consulta.setStatus(StatusConsulta.CANCELADA);
        consulta.setCancelReason(justificativa);
        appointmentRepository.save(consulta);

        auditLogService.log(emailProfissional, "RECUSA_AGENDAMENTO", "Appointment", id,
            "Justificativa: " + consulta.getCancelReason());
        emailTemplateService.notificarPacienteAgendamentoRecusado(consulta, consulta.getCancelReason());
    }

    @Transactional
    public void confirmarProfissional(@org.springframework.lang.NonNull Long id, String emailProfissional) {
        var consulta = appointmentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Consulta não encontrada"));

        if (!consulta.getProfessional().getEmail().equals(emailProfissional))
            throw new RuntimeException("Sem permissão para confirmar esta consulta.");

        // Removida a trava de 24h para confirmação, pois o profissional deve poder confirmar a qualquer momento.
        
        if (consulta.getStatus() == StatusConsulta.AGENDADA) {
            consulta.setStatus(StatusConsulta.CONFIRMADA_PROFISSIONAL);
            appointmentRepository.save(consulta);
        } else if (consulta.getStatus() == StatusConsulta.CONFIRMADA_PROFISSIONAL) {
            throw new RuntimeException("Consulta já confirmada pelo profissional.");
        } else {
            throw new RuntimeException("Esta consulta não pode ser confirmada no status atual.");
        }
    }

    @Transactional
    public void confirmarPaciente(@org.springframework.lang.NonNull Long id, String emailPaciente) {
        var consulta = appointmentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Consulta não encontrada"));

        if (!consulta.getPatient().getEmail().equals(emailPaciente))
            throw new RuntimeException("Sem permissão para confirmar esta consulta.");

        // Removida a trava de 24h para confirmação, pois o paciente deve poder confirmar a qualquer momento após o profissional.

        if (consulta.getStatus() == StatusConsulta.CONFIRMADA_PROFISSIONAL) {
            consulta.setStatus(StatusConsulta.CONFIRMADA);
            appointmentRepository.save(consulta);
            emailTemplateService.notificarConsultaConfirmada(consulta); // ✅ e-mail
        } else if (consulta.getStatus() == StatusConsulta.AGENDADA) {
            throw new RuntimeException("Aguardando confirmação do profissional primeiro.");
        } else if (consulta.getStatus() == StatusConsulta.CONFIRMADA) {
            throw new RuntimeException("Consulta já confirmada por ambos.");
        } else {
            throw new RuntimeException("Esta consulta não pode ser confirmada no status atual.");
        }
    }
}