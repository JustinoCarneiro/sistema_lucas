package com.sistema.lucas.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import com.sistema.lucas.model.dto.AppointmentCreateDTO;

@Entity
@Table(name = "appointments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "professional_id")
    private Professional professional;

    @ManyToOne
    @JoinColumn(name = "patient_id")
    private Patient patient;

    private LocalDateTime dateTime;
    private String reason;
    private String status; // Ex: SCHEDULED, COMPLETED, CANCELED

    public Appointment(Professional professional, Patient patient, AppointmentCreateDTO dto) {
        this.professional = professional;
        this.patient = patient;
        this.dateTime = dto.dateTime();
        this.reason = dto.reason();
        this.status = "SCHEDULED";
    }
}