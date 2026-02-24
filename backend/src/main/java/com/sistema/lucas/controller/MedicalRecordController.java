package com.sistema.lucas.controller;

import com.sistema.lucas.domain.*;
import com.sistema.lucas.dto.MedicalRecordRequestDTO;
import com.sistema.lucas.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/medical-records")
@RequiredArgsConstructor
public class MedicalRecordController {

    private final MedicalRecordRepository repository;
    private final AppointmentRepository appointmentRepository;

    @PostMapping
    @Transactional
    public ResponseEntity<Void> save(@RequestBody MedicalRecordRequestDTO dto) {
        Appointment app = appointmentRepository.findById(dto.appointmentId())
                .orElseThrow(() -> new RuntimeException("Consulta não encontrada"));

        MedicalRecord record = MedicalRecord.builder()
                .notes(dto.notes())
                .createdAt(LocalDateTime.now())
                .appointment(app)
                .patient(app.getPatient())
                .build();

        repository.save(record);

        // Opcional: Marcar a consulta como concluída automaticamente
        app.setStatus(com.sistema.lucas.domain.enums.AppointmentStatus.COMPLETED);
        appointmentRepository.save(app);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<MedicalRecord>> getPatientHistory(@PathVariable Long patientId) {
        return ResponseEntity.ok(repository.findByPatientIdOrderByCreatedAtDesc(patientId));
    }
}