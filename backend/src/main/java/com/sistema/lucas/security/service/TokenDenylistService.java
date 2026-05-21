package com.sistema.lucas.security.service;

import com.sistema.lucas.model.TokenDenylist;
import com.sistema.lucas.repository.TokenDenylistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TokenDenylistService {

    @Autowired
    private TokenDenylistRepository tokenDenylistRepository;

    public void revokeToken(@org.springframework.lang.NonNull String token, @org.springframework.lang.NonNull LocalDateTime expiresAt) {
        if (!tokenDenylistRepository.existsById(token)) {
            TokenDenylist denylistEntry = new TokenDenylist(token, expiresAt);
            tokenDenylistRepository.save(denylistEntry);
        }
    }

    public boolean isRevoked(@org.springframework.lang.NonNull String token) {
        return tokenDenylistRepository.existsById(token);
    }
}
