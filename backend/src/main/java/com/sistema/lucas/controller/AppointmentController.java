package com.sistema.lucas.controller;

import com.sistema.lucas.domain.Appointment;
import com.sistema.lucas.domain.User;
import com.sistema.lucas.domain.enums.AppointmentStatus;
import com.sistema.lucas.dto.AppointmentCreateDTO;
import com.sistema.lucas.dto.AppointmentResponseDTO;
import com.sistema.lucas.dto.PatientScheduleDTO;
import com.sistema.lucas.repository.AppointmentRepository;
import com.sistema.lucas.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService service;
    private final AppointmentRepository repository;

    @PostMapping
    public ResponseEntity<AppointmentResponseDTO> schedule(@RequestBody @Valid AppointmentCreateDTO dto) {
        Appointment appointment = service.schedule(dto);
        AppointmentResponseDTO response = new AppointmentResponseDTO(
                appointment.getId(),
                appointment.getDoctor().getName(),
                appointment.getPatient().getName(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getStatus()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<AppointmentResponseDTO>> listAll(Pageable pagination) {
        var page = repository.findAll(pagination).map(appointment -> new AppointmentResponseDTO(
                appointment.getId(),
                appointment.getDoctor().getName(),
                appointment.getPatient().getName(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getStatus()
        ));
        return ResponseEntity.ok(page);
    }

    @GetMapping("/me")
    public ResponseEntity<Page<AppointmentResponseDTO>> listMyAppointments(
            @AuthenticationPrincipal User loggedUser, 
            Pageable pagination) {
            
        var page = repository.findAllByPatientId(loggedUser.getId(), pagination)
                .map(appointment -> new AppointmentResponseDTO(
                        appointment.getId(),
                        appointment.getDoctor().getName(),
                        appointment.getPatient().getName(),
                        appointment.getStartTime(),
                        appointment.getEndTime(),
                        appointment.getStatus()
                ));
        return ResponseEntity.ok(page);
    }

    @PostMapping("/me")
    public ResponseEntity<AppointmentResponseDTO> scheduleMyAppointments(
            @RequestBody @Valid PatientScheduleDTO dto,
            @AuthenticationPrincipal User loggedUser) {

        AppointmentCreateDTO secureDto = new AppointmentCreateDTO(
                dto.doctorId(),
                loggedUser.getId(),
                dto.startTime(),
                dto.endTime(),
                dto.reason()
        );

        Appointment appointment = service.schedule(secureDto);
        AppointmentResponseDTO response = new AppointmentResponseDTO(
                appointment.getId(),
                appointment.getDoctor().getName(),
                appointment.getPatient().getName(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getStatus()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/me/{id}")
    public ResponseEntity<Void> cancelMyAppointment(
            @PathVariable Long id, 
            @AuthenticationPrincipal User loggedUser) {
        
        service.cancelPatientAppointment(id, loggedUser.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/doctor-me")
    public ResponseEntity<List<AppointmentResponseDTO>> listDoctorAppointments(
            @AuthenticationPrincipal User loggedUser) {
            
        var appointments = repository.findByDoctorIdOrderByStartTimeAsc(loggedUser.getId())
                .stream()
                .map(appointment -> new AppointmentResponseDTO(
                        appointment.getId(),
                        appointment.getDoctor().getName(),
                        appointment.getPatient().getName(),
                        appointment.getStartTime(),
                        appointment.getEndTime(),
                        appointment.getStatus()
                )).toList();
                
        return ResponseEntity.ok(appointments);
    }

    @GetMapping("/doctor/today")
    public ResponseEntity<List<AppointmentResponseDTO>> getTodayAppointments(
            @AuthenticationPrincipal User loggedUser) {
        
        LocalDateTime start = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime end = LocalDateTime.now().with(LocalTime.MAX);

        var appointments = repository.findByDoctorIdAndStartTimeBetweenOrderByStartTimeAsc(
                loggedUser.getId(), start, end)
                .stream()
                .map(app -> new AppointmentResponseDTO(
                        app.getId(),
                        app.getDoctor().getName(),
                        app.getPatient().getName(),
                        app.getStartTime(),
                        app.getEndTime(),
                        app.getStatus()
                )).toList();
                
        return ResponseEntity.ok(appointments);
    }

    @PatchMapping("/{id}/no-show")
    public ResponseEntity<Void> markAsNoShow(@PathVariable Long id) {
        service.updateStatus(id, AppointmentStatus.CANCELLED);
        return ResponseEntity.noContent().build();
    }
}