package com.sistema.lucas.service;

import com.sistema.lucas.domain.Appointment;
import com.sistema.lucas.domain.Professional;
import com.sistema.lucas.domain.Patient;
import com.sistema.lucas.domain.enums.AppointmentStatus;
import com.sistema.lucas.dto.appointment.AppointmentCreateDTO;
import com.sistema.lucas.repository.AppointmentRepository;
import com.sistema.lucas.repository.ProfessionalRepository;
import com.sistema.lucas.repository.PatientRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final ProfessionalRepository professionalRepository;
    private final PatientRepository patientRepository;
    private final NotificationService notificationService;

    @Transactional
    public Appointment schedule(AppointmentCreateDTO dto) {
        Professional professional = professionalRepository.findById(dto.professionalId())
            .orElseThrow(() -> new EntityNotFoundException("Médico não encontrado"));

        Patient patient = patientRepository.findById(dto.patientId())
            .orElseThrow(() -> new EntityNotFoundException("Paciente não encontrado"));

        boolean hasConflict = appointmentRepository.existsConflict(
            professional.getId(), dto.startTime(), dto.endTime()
        );

        if (hasConflict) {
            throw new IllegalArgumentException("O médico já possui agendamento neste horário.");
        }

        Appointment appointment = Appointment.builder()
            .professional(professional)
            .patient(patient)
            .startTime(dto.startTime())
            .endTime(dto.endTime())
            .reason(dto.reason())
            .status(AppointmentStatus.SCHEDULED)
            .build();

        Appointment savedAppointment = appointmentRepository.save(appointment);

        try {
            String formattedDate = savedAppointment.getStartTime()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm"));

            notificationService.sendAppointmentConfirmation(
                savedAppointment.getPatient().getEmail(),
                savedAppointment.getPatient().getName(),
                savedAppointment.getProfessional().getName(),
                formattedDate
            );

            String whatsappMsg = "Olá " + savedAppointment.getPatient().getName() + 
                                ", sua consulta com Dr(a). " + savedAppointment.getProfessional().getName() + 
                                " está confirmada para " + formattedDate + ".";
            notificationService.sendWhatsAppMessage(savedAppointment.getPatient().getWhatsapp(), whatsappMsg);

        } catch (Exception e) {
            System.err.println("Erro ao disparar notificações: " + e.getMessage());
        }

        return savedAppointment;
    }

    @Transactional
    public void cancelPatientAppointment(Long appointmentId, Long patientId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> new EntityNotFoundException("Consulta não encontrada."));

        if (!appointment.getPatient().getId().equals(patientId)) {
            throw new SecurityException("Acesso negado: Você não pode cancelar a consulta de outra pessoa.");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);
    }

    // 👇 MÉTODO ADICIONADO PARA RESOLVER O ERRO DO CONTROLLER 👇
    @Transactional
    public void updateStatus(Long id, AppointmentStatus status) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Consulta não encontrada"));
        
        appointment.setStatus(status);
        appointmentRepository.save(appointment);
    }
}