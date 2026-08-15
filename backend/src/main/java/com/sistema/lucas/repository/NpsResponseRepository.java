// backend/src/main/java/com/sistema/lucas/repository/NpsResponseRepository.java
package com.sistema.lucas.repository;

import com.sistema.lucas.model.NpsResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NpsResponseRepository extends JpaRepository<NpsResponse, Long> {
    Optional<NpsResponse> findByToken(String token);
    Optional<NpsResponse> findByAppointmentId(Long appointmentId);
}
