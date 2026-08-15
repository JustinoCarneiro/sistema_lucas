// backend/src/main/java/com/sistema/lucas/repository/WaitlistEntryRepository.java
package com.sistema.lucas.repository;

import com.sistema.lucas.model.WaitlistEntry;
import com.sistema.lucas.model.enums.WaitlistStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, Long> {

    Optional<WaitlistEntry> findByToken(String token);

    Optional<WaitlistEntry> findFirstByProfessionalIdAndDateTimeAndStatusOrderByCriadoEmAsc(
        Long professionalId, LocalDateTime dateTime, WaitlistStatus status);

    boolean existsByPatientIdAndProfessionalIdAndDateTimeAndStatus(
        Long patientId, Long professionalId, LocalDateTime dateTime, WaitlistStatus status);

    List<WaitlistEntry> findByPatientEmailOrderByCriadoEmDesc(String email);

    List<WaitlistEntry> findByStatusAndOfertaExpiraEmBefore(WaitlistStatus status, LocalDateTime momento);
}
