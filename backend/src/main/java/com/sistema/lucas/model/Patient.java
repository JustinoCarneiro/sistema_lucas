package com.sistema.lucas.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "patients")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String email;
    private String password;
    private String cpf;
    private String whatsapp;
    private String healthPlan;

    public Patient(PatientCreateDTO dto) {
        this.name = dto.name();
        this.email = dto.email();
        this.password = dto.password();
        this.cpf = dto.cpf();
        this.whatsapp = dto.whatsapp();
        this.healthPlan = dto.healthPlan();
    }
}