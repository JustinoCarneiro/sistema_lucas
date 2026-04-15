// backend/src/main/java/com/sistema/lucas/model/Appointment.java
package com.sistema.lucas.model;

import com.sistema.lucas.model.dto.AppointmentCreateDTO;
import com.sistema.lucas.model.enums.StatusConsulta;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointments", uniqueConstraints = {
    @UniqueConstraint(name = "uk_professional_datetime", columnNames = {"professional_id", "date_time"})
})
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

    @Enumerated(EnumType.STRING)
    private StatusConsulta status;

    public Appointment(Professional professional, Patient patient, LocalDateTime dateTime, String reason, StatusConsulta status) {
        this.professional = professional;
        this.patient = patient;
        this.dateTime = dateTime;
        this.reason = reason;
        this.status = status;
    }

    public Appointment(Professional professional, Patient patient, AppointmentCreateDTO dto) {
        this.professional = professional;
        this.patient = patient;
        this.dateTime = dto.dateTime();
        this.reason = dto.reason();
        this.status = StatusConsulta.AGENDADA;
    }
}