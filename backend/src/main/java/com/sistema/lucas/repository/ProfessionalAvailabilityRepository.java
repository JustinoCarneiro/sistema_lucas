// backend/src/main/java/com/sistema/lucas/repository/ProfessionalAvailabilityRepository.java
package com.sistema.lucas.repository;

import com.sistema.lucas.model.ProfessionalAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ProfessionalAvailabilityRepository extends JpaRepository<ProfessionalAvailability, Long> {

    List<ProfessionalAvailability> findByProfessionalId(Long professionalId);

    List<ProfessionalAvailability> findByProfessionalEmail(String email);

    List<ProfessionalAvailability> findByProfessionalEmailAndDate(String email, LocalDate date);

    @Modifying
    @Query("DELETE FROM ProfessionalAvailability a WHERE a.professional.email = :email AND a.date >= :startDate AND a.date <= :endDate")
    void deleteByProfessionalEmailAndDateBetween(String email, LocalDate startDate, LocalDate endDate);

    boolean existsByProfessionalIdAndDateBetween(Long professionalId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT DISTINCT a.date FROM ProfessionalAvailability a WHERE a.professional.id = :professionalId AND a.date > :from ORDER BY a.date")
    List<LocalDate> findDistinctFutureDatesByProfessionalId(@Param("professionalId") Long professionalId, @Param("from") LocalDate from);

    // Profissionais que possuem pelo menos uma entrada de disponibilidade
    @Query("SELECT DISTINCT a.professional.id FROM ProfessionalAvailability a")
    List<Long> findProfessionalIdsComDisponibilidade();
}
