package com.sistema.lucas.service;

import com.sistema.lucas.model.*;
import com.sistema.lucas.model.dto.AppointmentCreateDTO;
import com.sistema.lucas.model.dto.AppointmentResponseDTO;
import com.sistema.lucas.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class AppointmentService {

    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private ProfessionalRepository professionalRepository;
    @Autowired private PatientRepository patientRepository;

    public List<AppointmentResponseDTO> findAll() {
        return appointmentRepository.findAll().stream().map(AppointmentResponseDTO::new).toList();
    }

    @Transactional
    public void schedule(AppointmentCreateDTO dto, String patientEmail) {
        var professional = professionalRepository.findById(dto.professionalId())
            .orElseThrow(() -> new RuntimeException("Profissional não encontrado"));
            
        // ✅ CORREÇÃO: Tratando o Optional<Patient> do findByEmail
        var patient = patientRepository.findByEmail(patientEmail)
            .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));

        var appointment = new Appointment(professional, patient, dto);
        appointmentRepository.save(appointment);
    }

    public List<AppointmentResponseDTO> getMyAppointments(String email) {
        return appointmentRepository.findByPatientEmail(email).stream().map(AppointmentResponseDTO::new).toList();
    }

    // ✅ ADICIONADO: Agenda completa do profissional logado
    public List<AppointmentResponseDTO> getMyProfessionalAppointments(String email) {
        return appointmentRepository.findByProfessionalEmail(email).stream().map(AppointmentResponseDTO::new).toList();
    }

    @Transactional
    public void cancel(Long id) {
        var appointment = appointmentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Consulta não encontrada"));
        appointment.setStatus("CANCELLED");
        appointmentRepository.save(appointment);
    }
}