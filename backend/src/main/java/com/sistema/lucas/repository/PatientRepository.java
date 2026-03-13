package com.sistema.lucas.repository;

import com.sistema.lucas.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional; // <--- ADICIONE ESTA LINHA

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    boolean existsByCpf(String cpf);
    boolean existsByEmail(String email);

    Optional<Patient> findByEmail(String email);
}