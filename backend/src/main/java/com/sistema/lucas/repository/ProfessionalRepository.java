package com.sistema.lucas.repository;

import com.sistema.lucas.domain.Professional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfessionalRepository extends JpaRepository<Professional, Long> {
    // O Spring cria o SQL automaticamente para estes métodos:
    boolean existsByCrm(String crm);
    boolean existsByEmail(String email);
}