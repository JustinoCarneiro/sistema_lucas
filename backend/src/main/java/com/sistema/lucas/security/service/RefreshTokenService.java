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

    // Versão atômica de isValid()+markAsUsed(): faz a checagem de "ainda não usado" e a marcação
    // como usado num único UPDATE condicional no banco, fechando a corrida (TOCTOU) entre duas
    // requisições concorrentes de /auth/refresh com o mesmo token. Retorna true só pra quem
    // "vencer" a corrida — a outra chamada recebe false e deve ser recusada.
    @Transactional
    public boolean consumirSeValido(RefreshToken token) {
        if (token.getRevokedAt() != null || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }
        return refreshTokenRepository.marcarUsadoSeAindaNaoUsado(token.getToken()) > 0;
    }

    @Transactional
    public void revoke(RefreshToken token) {
        token.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(token);
    }
}
