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
import java.time.format.DateTimeFormatter; // Para deixar a data bonita no e-mail

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final NotificationService notificationService; // 1. Injetamos o serviço de notificações

    @Transactional
    public Appointment schedule(AppointmentCreateDTO dto) {
        // 1. Buscar Médico
        Doctor doctor = doctorRepository.findById(dto.doctorId())
            .orElseThrow(() -> new EntityNotFoundException("Médico não encontrado"));

        // 2. Buscar Paciente
        Patient patient = patientRepository.findById(dto.patientId())
            .orElseThrow(() -> new EntityNotFoundException("Paciente não encontrado"));

        // 3. Validar Conflito de Horário
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

        Appointment savedAppointment = appointmentRepository.save(appointment);

        // 👇 5. DISPARO DA NOTIFICAÇÃO (E-mail e Simulação WhatsApp) 👇
        try {
            String formattedDate = savedAppointment.getStartTime()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm"));

            // Envia E-mail (O @Async no NotificationService garante que não trave aqui)
            notificationService.sendAppointmentConfirmation(
                savedAppointment.getPatient().getEmail(),
                savedAppointment.getPatient().getName(),
                savedAppointment.getDoctor().getName(),
                formattedDate
            );

            // Simula WhatsApp (Próxima etapa do Card 17)
            String whatsappMsg = "Olá " + savedAppointment.getPatient().getName() + 
                                ", sua consulta com Dr(a). " + savedAppointment.getDoctor().getName() + 
                                " está confirmada para " + formattedDate + ".";
            notificationService.sendWhatsAppMessage(savedAppointment.getPatient().getWhatsapp(), whatsappMsg);

        } catch (Exception e) {
            // Logamos o erro, mas não cancelamos a transação da consulta se o e-mail falhar
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

        // Opcional: Notificar o cancelamento também
        // notificationService.sendGenericEmail(appointment.getPatient().getEmail(), "Consulta Cancelada", "Sua consulta foi cancelada.");
    }
}