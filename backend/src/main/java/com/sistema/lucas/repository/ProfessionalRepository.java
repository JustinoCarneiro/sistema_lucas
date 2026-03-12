package com.sistema.lucas.repository;

import com.sistema.lucas.model.Professional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfessionalRepository extends JpaRepository<Professional, Long> {
    // Método para validar se o CRM já existe antes de salvar
    boolean existsByCrm(String crm);
}