package com.sistema.lucas.repository;

import com.sistema.lucas.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional; // <--- ADICIONE ESTA LINHA

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    boolean existsByCpf(String cpf);
    boolean existsByEmail(String email);

    // AUD-11: Verificação de unicidade de CPF via hash (sem carregar todos em memória)
    Optional<Patient> findByCpfHash(String cpfHash);
    boolean existsByCpfHash(String cpfHash);

    Optional<Patient> findByEmail(String email);
}