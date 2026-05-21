package com.sistema.lucas.repository;

import com.sistema.lucas.model.RefreshToken;
import com.sistema.lucas.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByUser(User user);
    long deleteByExpiresAtBeforeOrUsedTrueOrRevokedAtNotNull(LocalDateTime now);
}
