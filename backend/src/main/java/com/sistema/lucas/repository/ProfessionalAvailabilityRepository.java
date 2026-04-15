// backend/src/main/java/com/sistema/lucas/repository/ProfessionalAvailabilityRepository.java
package com.sistema.lucas.repository;

import com.sistema.lucas.model.ProfessionalAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProfessionalAvailabilityRepository extends JpaRepository<ProfessionalAvailability, Long> {

    List<ProfessionalAvailability> findByProfessionalId(Long professionalId);

    List<ProfessionalAvailability> findByProfessionalEmail(String email);

    List<ProfessionalAvailability> findByProfessionalEmailAndDayOfWeek(String email, DayOfWeek dayOfWeek);

    @Modifying
    @Query("DELETE FROM ProfessionalAvailability a WHERE a.professional.email = :email AND a.dayOfWeek = :dayOfWeek")
    void deleteByProfessionalEmailAndDayOfWeek(String email, DayOfWeek dayOfWeek);

    // Profissionais que possuem pelo menos uma entrada de disponibilidade
    @Query("SELECT DISTINCT a.professional.id FROM ProfessionalAvailability a")
    List<Long> findProfessionalIdsComDisponibilidade();
}
