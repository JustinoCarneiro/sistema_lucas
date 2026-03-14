// backend/src/main/java/com/sistema/lucas/service/ProntuarioService.java
package com.sistema.lucas.service;

import com.sistema.lucas.model.Prontuario;
import com.sistema.lucas.model.enums.StatusConsulta; // ✅ import adicionado
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

    public List<Prontuario> getByPatientId(Long patientId) {
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

        return prontuarioRepository.save(prontuario);
    }
}