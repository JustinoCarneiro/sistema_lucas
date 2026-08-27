package com.sistema.lucas.security.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.sistema.lucas.model.User; // Adicione este import
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class TokenService {
    @Value("${api.security.token.secret}")
    private String secret;

    // 1. Mudamos de (String email) para (User user)
    public String generateToken(User user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("auth-api")
                    .withSubject(user.getEmail()) // Usa o email do objeto
                    .withClaim("role", user.getRole().name())
                    .withClaim("verified", user.isVerified()) // ✅ NOVO
                    .withExpiresAt(genExpirationDate())
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro ao gerar token", exception);
        }
    }

    public String validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("auth-api")
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException exception) {
            return "";
        }
    }

    // Instant.now() é um ponto absoluto no tempo, sem depender do fuso horário local da JVM —
    // evita o bug de LocalDateTime.now() (wall-clock) interpretado com um offset -03:00
    // hardcoded quando o container roda em UTC, o que fazia o token durar ~3h a mais dos 15min
    // documentados.
    private Instant genExpirationDate() {
        return Instant.now().plusSeconds(15 * 60);
    }
}