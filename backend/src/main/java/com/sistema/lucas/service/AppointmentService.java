package com.sistema.lucas.service;

import com.sistema.lucas.domain.Appointment;
import com.sistema.lucas.domain.Doctor;
import com.sistema.lucas.domain.Patient;
import com.sistema.lucas.domain.enums.AppointmentStatus;
import com.sistema.lucas.dto.AppointmentCreateDTO;
import com.sistema.lucas.repository.AppointmentRepository;
import com.sistema.lucas.repository.DoctorRepository;
import com.sistema.lucas.repository.PatientRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    @Transactional
    public Appointment schedule(AppointmentCreateDTO dto) {
        // 1. Buscar Médico (Se não achar, erro 404)
        Doctor doctor = doctorRepository.findById(dto.doctorId())
            .orElseThrow(() -> new EntityNotFoundException("Médico não encontrado"));

        // 2. Buscar Paciente
        Patient patient = patientRepository.findById(dto.patientId())
            .orElseThrow(() -> new EntityNotFoundException("Paciente não encontrado"));

        // 3. Validar Conflito de Horário (A Regra de Ouro)
        boolean hasConflict = appointmentRepository.existsConflict(
            doctor.getId(), dto.startTime(), dto.endTime()
        );

        if (hasConflict) {
            throw new IllegalArgumentException("O médico já possui agendamento neste horário.");
        }

        // 4. Criar e Salvar
        Appointment appointment = Appointment.builder()
            .doctor(doctor)
            .patient(patient)
            .startTime(dto.startTime())
            .endTime(dto.endTime())
            .reason(dto.reason())
            .status(AppointmentStatus.SCHEDULED)
            .build();

        return appointmentRepository.save(appointment);
    }

    @Transactional
    public void cancelPatientAppointment(Long appointmentId, Long patientId) {
        // 1. Procura a consulta
        Appointment appointment = appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> new EntityNotFoundException("Consulta não encontrada."));

        // 2. Regra de Segurança: O paciente logado é o dono desta consulta?
        if (!appointment.getPatient().getId().equals(patientId)) {
            throw new SecurityException("Acesso negado: Você não pode cancelar a consulta de outra pessoa.");
        }

        // 3. Muda o estado para CANCELADO
        appointment.setStatus(AppointmentStatus.CANCELLED);
        
        // Como tem @Transactional, o Spring já atualiza automaticamente no banco,
        // mas podemos colocar o save por garantia visual:
        appointmentRepository.save(appointment);
    }
}