package com.sistema.lucas.repository;

import com.sistema.lucas.model.TokenDenylist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface TokenDenylistRepository extends JpaRepository<TokenDenylist, String> {
    long deleteByExpiresAtBefore(LocalDateTime now);
}
