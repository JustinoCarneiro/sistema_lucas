package com.sistema.lucas.controller;

import com.sistema.lucas.domain.Appointment;
import com.sistema.lucas.dto.AppointmentCreateDTO;
import com.sistema.lucas.dto.AppointmentResponseDTO;
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
}