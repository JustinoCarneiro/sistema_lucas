// backend/src/main/java/com/sistema/lucas/service/AppointmentService.java
package com.sistema.lucas.service;

import com.sistema.lucas.model.*;
import com.sistema.lucas.model.dto.AppointmentCreateDTO;
import com.sistema.lucas.model.dto.AppointmentResponseDTO;
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

    // ─── Leitura ─────────────────────────────────────────────────────────────

    public List<AppointmentResponseDTO> findAll() {
        return appointmentRepository.findAll().stream().map(AppointmentResponseDTO::new).toList();
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

    public AppointmentResponseDTO buscarPorId(Long id, String email) {
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
        var profissional = professionalRepository.findById(dto.professionalId())
            .orElseThrow(() -> new RuntimeException("Profissional não encontrado"));
        var paciente = patientRepository.findByEmail(emailPaciente)
            .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));

        // ✅ Validar que o profissional atende nesse dia da semana
        var dayOfWeek = dto.dateTime().getDayOfWeek();
        var availability = availabilityRepository
            .findByProfessionalEmailAndDayOfWeek(profissional.getEmail(), dayOfWeek);
        
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
        emailTemplateService.notificarConsultaAgendada(consulta); // ✅ e-mail
    }

    // ─── Cancelamento ────────────────────────────────────────────────────────

    @Transactional
    public void cancelar(Long id, String email) {
        var consulta = appointmentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Consulta não encontrada"));
            
        // 🛡️ Segurança (IDOR) - Permitir Paciente, Profissional ou ADMIN
        var usuarioAcao = userRepository.findByEmail(email);
        boolean isOwner = consulta.getPatient().getEmail().equals(email) || consulta.getProfessional().getEmail().equals(email);
        boolean isAdmin = usuarioAcao != null && usuarioAcao.getRole() == com.sistema.lucas.model.enums.Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new RuntimeException("Operação de Segurança: Tentativa de cancelamento malicioso bloqueada.");
        }
        
        consulta.setStatus(StatusConsulta.CANCELADA);
        appointmentRepository.save(consulta);
        emailTemplateService.notificarConsultaCancelada(consulta); // ✅ e-mail
    }

    // ─── Falta ───────────────────────────────────────────────────────────────

    @Transactional
    public void marcarFalta(Long id, String emailProfissional) {
        var consulta = appointmentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Consulta não encontrada"));
            
        // 🛡️ Segurança (IDOR)
        if (!consulta.getProfessional().getEmail().equals(emailProfissional)) {
            throw new RuntimeException("Operação de Segurança: Você não é o médico dessa consulta.");
        }
        consulta.setStatus(StatusConsulta.FALTA);
        appointmentRepository.save(consulta);
    }

    // ─── Confirmação (Profissional primeiro, Paciente depois) ────────────────

    // Regra: só permite ação se faltam mais de 24h para a consulta
    private void validarJanela(Appointment consulta) {
        if (LocalDateTime.now().isAfter(consulta.getDateTime().minusHours(24))) {
            throw new RuntimeException("Não é possível alterar a consulta com menos de 24h de antecedência.");
        }
    }

    @Transactional
    public void confirmarProfissional(Long id, String emailProfissional) {
        var consulta = appointmentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Consulta não encontrada"));

        if (!consulta.getProfessional().getEmail().equals(emailProfissional))
            throw new RuntimeException("Sem permissão para confirmar esta consulta.");

        validarJanela(consulta);

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
    public void confirmarPaciente(Long id, String emailPaciente) {
        var consulta = appointmentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Consulta não encontrada"));

        if (!consulta.getPatient().getEmail().equals(emailPaciente))
            throw new RuntimeException("Sem permissão para confirmar esta consulta.");

        validarJanela(consulta);

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