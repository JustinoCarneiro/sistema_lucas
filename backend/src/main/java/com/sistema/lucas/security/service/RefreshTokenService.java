package com.sistema.lucas.security.service;

import com.sistema.lucas.model.RefreshToken;
import com.sistema.lucas.model.User;
import com.sistema.lucas.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public String createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7)); // Validade de 7 dias
        refreshToken.setUsed(false);
        
        refreshTokenRepository.save(refreshToken);
        return refreshToken.getToken();
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public boolean isValid(RefreshToken token) {
        if (token.isUsed() || token.getRevokedAt() != null) {
            return false;
        }
        return !token.getExpiresAt().isBefore(LocalDateTime.now());
    }

    @Transactional
    public void markAsUsed(RefreshToken token) {
        token.setUsed(true);
        refreshTokenRepository.save(token);
    }

    @Transactional
    public void revoke(RefreshToken token) {
        token.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(token);
    }
}
