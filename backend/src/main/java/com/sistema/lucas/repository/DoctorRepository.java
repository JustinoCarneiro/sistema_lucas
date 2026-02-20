package com.sistema.lucas.repository;

import com.sistema.lucas.domain.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    // O Spring cria o SQL automaticamente para estes métodos:
    boolean existsByCrm(String crm);
    boolean existsByEmail(String email);
}