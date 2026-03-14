// backend/src/main/java/com/sistema/lucas/repository/PasswordResetTokenRepository.java
package com.sistema.lucas.repository;

import com.sistema.lucas.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    void deleteByUserId(Long userId);
}