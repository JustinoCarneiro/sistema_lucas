// backend/src/main/java/com/sistema/lucas/repository/ProntuarioRepository.java
package com.sistema.lucas.repository;

import com.sistema.lucas.model.Prontuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProntuarioRepository extends JpaRepository<Prontuario, Long> {
    List<Prontuario> findByPatientIdOrderByCriadoEmDesc(Long patientId);
    List<Prontuario> findByProfessionalEmailOrderByCriadoEmDesc(String email);
}