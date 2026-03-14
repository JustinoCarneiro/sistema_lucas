package com.sistema.lucas.service;

import com.sistema.lucas.model.*;
import com.sistema.lucas.model.dto.AppointmentCreateDTO;
import com.sistema.lucas.model.dto.AppointmentResponseDTO; // IMPORTANTE: Importar o novo DTO
import com.sistema.lucas.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private ProfessionalRepository professionalRepository;

    @Autowired
    private PatientRepository patientRepository;

    // Ajustado para retornar DTO e evitar recursão infinita no JSON
    public List<AppointmentResponseDTO> findAll() {
        return appointmentRepository.findAll()
                .stream()
                .map(AppointmentResponseDTO::new)
                .toList();
    }

    @Transactional
    public void schedule(AppointmentCreateDTO dto) {
        var professional = professionalRepository.findById(dto.professionalId())
            .orElseThrow(() -> new RuntimeException("Profissional não encontrado"));
            
        var patient = patientRepository.findById(dto.patientId())
            .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));

        var appointment = new Appointment(professional, patient, dto);
        appointmentRepository.save(appointment);
    }

    public List<AppointmentResponseDTO> getTodayAppointments(String email) {
        return appointmentRepository.findTodayAppointmentsByProfessionalEmail(email)
                .stream()
                .map(AppointmentResponseDTO::new)
                .toList();
    }

    public List<AppointmentResponseDTO> getMyAppointments(String email) {
        return appointmentRepository.findByPatientEmail(email)
                .stream()
                .map(AppointmentResponseDTO::new)
                .toList();
    }
}