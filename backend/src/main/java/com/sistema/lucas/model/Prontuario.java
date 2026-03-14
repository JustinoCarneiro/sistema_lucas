// backend/src/main/java/com/sistema/lucas/model/Prontuario.java
package com.sistema.lucas.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "prontuarios")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Prontuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @ManyToOne
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @ManyToOne
    @JoinColumn(name = "professional_id")
    private Professional professional;

    @Column(columnDefinition = "TEXT")
    private String notas;

    private LocalDateTime criadoEm;

    @PrePersist
    protected void onCreate() {
        this.criadoEm = LocalDateTime.now();
    }
}