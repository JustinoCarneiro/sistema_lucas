// backend/src/main/java/com/sistema/lucas/model/WaitlistEntry.java
package com.sistema.lucas.model;

import com.sistema.lucas.model.enums.WaitlistStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "waitlist_entries")
@Getter
@Setter
@NoArgsConstructor
public class WaitlistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "professional_id", nullable = false)
    private Professional professional;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "date_time", nullable = false)
    private LocalDateTime dateTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WaitlistStatus status = WaitlistStatus.AGUARDANDO;

    // Criada só quando a vaga é ofertada (status OFERECIDA) — é ela quem de fato
    // ocupa o horário, impedindo que outro paciente agende por cima.
    @OneToOne
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @Column(unique = true)
    private String token;

    @Column(nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    private LocalDateTime ofertaExpiraEm;

    public boolean isOfertaExpirada() {
        return ofertaExpiraEm != null && LocalDateTime.now().isAfter(ofertaExpiraEm);
    }
}
