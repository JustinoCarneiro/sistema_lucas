// backend/src/main/java/com/sistema/lucas/model/ProfessionalAvailability.java
package com.sistema.lucas.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "professional_availability",
       uniqueConstraints = @UniqueConstraint(columnNames = {"professional_id", "available_date", "startTime"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ProfessionalAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "professional_id", nullable = false)
    private Professional professional;

    @Column(name = "available_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;
}
