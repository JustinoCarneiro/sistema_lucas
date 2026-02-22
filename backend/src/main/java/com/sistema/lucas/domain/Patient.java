package com.sistema.lucas.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "patients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@PrimaryKeyJoinColumn(name = "user_id")
public class Patient extends User {

    @NotBlank
    @Column(nullable = false, length = 14, unique = true)
    private String cpf;

    @Column(name = "health_insurance")
    private String healthInsurance; // Ex: Unimed, Amil

    @Column(name = "whatsapp")
    private String whatsapp;
}