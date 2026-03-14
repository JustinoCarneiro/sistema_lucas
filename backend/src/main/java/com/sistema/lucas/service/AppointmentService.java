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
import java.util.List;

@Service
public class AppointmentService {

    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private ProfessionalRepository professionalRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private EmailTemplateService emailTemplateService;

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

    public AppointmentResponseDTO buscarPorId(Long id) {
        return appointmentRepository.findById(id)
            .map(AppointmentResponseDTO::new)
            .orElseThrow(() -> new RuntimeException("Consulta não encontrada"));
    }

    // ─── Agendamento ─────────────────────────────────────────────────────────

    // Apenas pacientes agendam — via token JWT
    @Transactional
    public void agendar(AppointmentCreateDTO dto, String emailPaciente) {
        var profissional = professionalRepository.findById(dto.professionalId())
            .orElseThrow(() -> new RuntimeException("Profissional não encontrado"));
        var paciente = patientRepository.findByEmail(emailPaciente)
            .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));

        var consulta = new Appointment(profissional, paciente, dto);
        appointmentRepository.save(consulta);
        emailTemplateService.notificarConsultaAgendada(consulta); // ✅ e-mail
    }

    // ─── Cancelamento ────────────────────────────────────────────────────────

    @Transactional
    public void cancelar(Long id) {
        var consulta = appointmentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Consulta não encontrada"));
        consulta.setStatus(StatusConsulta.CANCELADA);
        appointmentRepository.save(consulta);
        emailTemplateService.notificarConsultaCancelada(consulta); // ✅ e-mail
    }

    // ─── Falta ───────────────────────────────────────────────────────────────

    @Transactional
    public void marcarFalta(Long id) {
        var consulta = appointmentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Consulta não encontrada"));
        consulta.setStatus(StatusConsulta.FALTA);
        appointmentRepository.save(consulta);
    }

    // ─── Confirmação ─────────────────────────────────────────────────────────

    // Regra: só permite ação se faltam mais de 24h para a consulta
    private void validarJanela(Appointment consulta) {
        if (LocalDateTime.now().isAfter(consulta.getDateTime().minusHours(24))) {
            throw new RuntimeException("Não é possível alterar a consulta com menos de 24h de antecedência.");
        }
    }

    @Transactional
    public void confirmarPaciente(Long id, String emailPaciente) {
        var consulta = appointmentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Consulta não encontrada"));

        if (!consulta.getPatient().getEmail().equals(emailPaciente))
            throw new RuntimeException("Sem permissão para confirmar esta consulta.");

        validarJanela(consulta);

        if (consulta.getStatus() == StatusConsulta.AGENDADA) {
            consulta.setStatus(StatusConsulta.CONFIRMADA_PACIENTE);
            appointmentRepository.save(consulta);
        } else if (consulta.getStatus() == StatusConsulta.CONFIRMADA_PACIENTE) {
            throw new RuntimeException("Consulta já confirmada pelo paciente.");
        } else {
            throw new RuntimeException("Esta consulta não pode ser confirmada no status atual.");
        }
    }

    @Transactional
    public void confirmarProfissional(Long id, String emailProfissional) {
        var consulta = appointmentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Consulta não encontrada"));

        if (!consulta.getProfessional().getEmail().equals(emailProfissional))
            throw new RuntimeException("Sem permissão para confirmar esta consulta.");

        validarJanela(consulta);

        if (consulta.getStatus() == StatusConsulta.CONFIRMADA_PACIENTE) {
            consulta.setStatus(StatusConsulta.CONFIRMADA);
            appointmentRepository.save(consulta);
            emailTemplateService.notificarConsultaConfirmada(consulta); // ✅ e-mail
        } else if (consulta.getStatus() == StatusConsulta.AGENDADA) {
            throw new RuntimeException("Aguardando confirmação do paciente primeiro.");
        } else if (consulta.getStatus() == StatusConsulta.CONFIRMADA) {
            throw new RuntimeException("Consulta já confirmada por ambos.");
        } else {
            throw new RuntimeException("Esta consulta não pode ser confirmada no status atual.");
        }
    }
}