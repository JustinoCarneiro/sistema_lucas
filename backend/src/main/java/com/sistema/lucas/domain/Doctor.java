package com.sistema.lucas.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "doctors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@PrimaryKeyJoinColumn(name = "user_id") // <--- Chave estrangeira para a tabela users
public class Doctor extends User { // <--- Mudou de BaseEntity para User

    @NotBlank
    @Column(nullable = false, length = 20, unique = true)
    private String crm;

    @Column(name = "specialty")
    private String specialty;

    // Construtor utilitário para facilitar (opcional)
    // O Lombok @SuperBuilder seria ideal aqui, mas vamos manter simples por enquanto
}