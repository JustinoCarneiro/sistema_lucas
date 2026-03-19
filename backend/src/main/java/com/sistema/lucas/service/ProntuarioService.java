// backend/src/main/java/com/sistema/lucas/service/ProntuarioService.java
package com.sistema.lucas.service;

import com.sistema.lucas.model.Prontuario;
import com.sistema.lucas.model.enums.StatusConsulta;
import com.sistema.lucas.repository.AppointmentRepository;
import com.sistema.lucas.repository.ProntuarioRepository;
import com.sistema.lucas.repository.ProfessionalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ProntuarioService {

    @Autowired private ProntuarioRepository prontuarioRepository;
    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private ProfessionalRepository professionalRepository;
    @Autowired private AuditLogService auditLogService;

    public List<Prontuario> getByPatientId(Long patientId, String userEmail) {
        auditLogService.log(userEmail, "VISUALIZACAO", "Prontuario", patientId, "Visualizou histórico de prontuários do paciente ID: " + patientId);
        return prontuarioRepository.findByPatientIdOrderByCriadoEmDesc(patientId);
    }

    @Transactional
    public Prontuario create(Long appointmentId, String notas, String professionalEmail) {
        var appointment = appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> new RuntimeException("Consulta não encontrada"));

        var professional = professionalRepository.findByEmail(professionalEmail)
            .orElseThrow(() -> new RuntimeException("Profissional não encontrado"));

        appointment.setStatus(StatusConsulta.CONCLUIDA);
        appointmentRepository.save(appointment);

        var prontuario = new Prontuario();
        prontuario.setAppointment(appointment);
        prontuario.setPatient(appointment.getPatient());
        prontuario.setProfessional(professional);
        prontuario.setNotas(notas);

        Prontuario saved = prontuarioRepository.save(prontuario);
        auditLogService.log(professionalEmail, "CRIACAO", "Prontuario", saved.getId(), "Criou prontuário para consulta ID: " + appointmentId);
        return saved;
    }
}