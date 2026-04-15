// backend/src/main/java/com/sistema/lucas/model/ProfessionalAvailability.java
package com.sistema.lucas.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(name = "professional_availability",
       uniqueConstraints = @UniqueConstraint(columnNames = {"professional_id", "day_of_week", "startTime"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ProfessionalAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "professional_id", nullable = false)
    private Professional professional;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;
}
