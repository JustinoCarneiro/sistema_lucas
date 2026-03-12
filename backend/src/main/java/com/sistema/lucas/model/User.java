package com.sistema.lucas.model;

import com.sistema.lucas.model.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor // Gera construtor vazio (obrigatório para JPA)
@AllArgsConstructor // Gera construtor com todos os campos
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    // Construtor customizado que você está chamando no LucasApplication
    public User(String email, String password, Role role) {
        this.email = email;
        this.password = password;
        this.role = role;
    }
}