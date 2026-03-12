package com.sistema.lucas.model;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Professional extends User { // Herança estabelecida aqui
    private String crm;
    private String specialty;
}