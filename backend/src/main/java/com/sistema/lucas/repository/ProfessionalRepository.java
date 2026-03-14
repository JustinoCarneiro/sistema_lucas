// backend/src/main/java/com/sistema/lucas/repository/ProfessionalRepository.java
package com.sistema.lucas.repository;

import com.sistema.lucas.model.Professional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ProfessionalRepository extends JpaRepository<Professional, Long> {
    Optional<Professional> findByEmail(String email);
    boolean existsByRegistroConselho(String registroConselho); // ✅ era existsByCrm
}