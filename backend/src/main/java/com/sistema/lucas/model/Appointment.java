// backend/src/main/java/com/sistema/lucas/model/Appointment.java
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
    private String status;

    // ✅ NOVO CONSTRUTOR: Para uso no DataInitializer (Sem o ID)
    public Appointment(Professional professional, Patient patient, LocalDateTime dateTime, String reason, String status) {
        this.professional = professional;
        this.patient = patient;
        this.dateTime = dateTime;
        this.reason = reason;
        this.status = status;
    }

    // Construtor para o DTO (Já existente)
    public Appointment(Professional professional, Patient patient, AppointmentCreateDTO dto) {
        this.professional = professional;
        this.patient = patient;
        this.dateTime = dto.dateTime();
        this.reason = dto.reason();
        this.status = "SCHEDULED";
    }
}