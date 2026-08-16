// backend/src/main/java/com/sistema/lucas/model/NpsResponse.java
package com.sistema.lucas.model;

import com.sistema.lucas.config.jpa.EncryptionConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "nps_responses")
@Getter
@Setter
@NoArgsConstructor
public class NpsResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(nullable = false, unique = true)
    private String token;

    private Integer score;

    // Texto livre do paciente sobre o atendimento — mesma categoria de dado sensível que
    // Appointment.reason/cancelReason (motivo/justificativa de consulta), portanto cifrado.
    @Column(columnDefinition = "TEXT")
    @Convert(converter = EncryptionConverter.class)
    private String comentario;

    @Column(nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    private LocalDateTime respondidoEm;

    @Column(nullable = false)
    private LocalDateTime expiraEm;

    public boolean isExpirado() {
        return LocalDateTime.now().isAfter(expiraEm);
    }

    public boolean isRespondido() {
        return respondidoEm != null;
    }
}
