package com.sistema.lucas.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "professionals")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Professional {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String email;
    private String password;
    private String crm;
    private String specialty;

    public Professional(ProfessionalCreateDTO dto) {
        this.name = dto.name();
        this.email = dto.email();
        this.password = dto.password();
        this.crm = dto.crm();
        this.specialty = dto.specialty();
    }
}