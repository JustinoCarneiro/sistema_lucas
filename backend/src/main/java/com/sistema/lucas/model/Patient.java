package com.sistema.lucas.model;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Patient extends User { // Herança estabelecida aqui
    private String cpf;
    private String phone;
    private String insurance;
}