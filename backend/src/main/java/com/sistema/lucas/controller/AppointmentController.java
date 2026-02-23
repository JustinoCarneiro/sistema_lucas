package com.sistema.lucas.controller;

import com.sistema.lucas.domain.Appointment;
import com.sistema.lucas.dto.AppointmentCreateDTO;
import com.sistema.lucas.dto.AppointmentResponseDTO;
import com.sistema.lucas.dto.PatientScheduleDTO;
import com.sistema.lucas.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import com.sistema.lucas.repository.AppointmentRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.sistema.lucas.domain.User;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService service;
    private final AppointmentRepository repository;

    @PostMapping
    public ResponseEntity<AppointmentResponseDTO> schedule(@RequestBody @Valid AppointmentCreateDTO dto) {
        
        // 1. Chama o Service para salvar (aqui roda a regra de conflito de horário)
        Appointment appointment = service.schedule(dto);

        // 2. Monta o recibo de saída (ResponseDTO)
        AppointmentResponseDTO response = new AppointmentResponseDTO(
                appointment.getId(),
                appointment.getDoctor().getName(),
                appointment.getPatient().getName(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getStatus()
        );

        // 3. Retorna Status 201 (Created)
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<AppointmentResponseDTO>> listAll(Pageable pagination) {
        // Exemplo simples usando o repositório e convertendo para DTO
        // Se preferir, pode criar este método dentro do AppointmentService!
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
            @AuthenticationPrincipal User loggedUser, // Puxa o utilizador diretamente do Token!
            Pageable pagination) {
            
        // Busca no banco APENAS as consultas onde o patient_id for igual ao ID do token logado
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

        // 1. Criamos o DTO original preenchendo o patientId com o ID do utilizador logado de forma segura!
        AppointmentCreateDTO secureDto = new AppointmentCreateDTO(
                dto.doctorId(),
                loggedUser.getId(), // <-- O HACKER NÃO CONSEGUE MUDAR ISTO!
                dto.startTime(),
                dto.endTime(),
                dto.reason()
        );

        // 2. Usamos o mesmo serviço que a clínica usa (com a mesma regra de conflito de horários)
        Appointment appointment = service.schedule(secureDto);

        // 3. Montamos a resposta
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
        
        // Manda o ID da consulta e o ID do paciente (seguro via Token) para o serviço
        service.cancelPatientAppointment(id, loggedUser.getId());
        
        return ResponseEntity.noContent().build(); // Retorna 204 (Sucesso, sem conteúdo)
    }
}