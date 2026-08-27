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

    // MFA (SEC-02): token curto emitido no login quando o usuário tem mfaEnabled=true, ANTES do
    // segundo fator ser conferido. Claim "mfaPending" é o que distingue esse token do token de
    // sessão real — SecurityFilter só lê o cookie "token" (não "mfa_pending_token"), então isso
    // já é inerte pro resto do sistema por si só; mas validateMfaPendingToken() também exige
    // essa claim explicitamente, pra um token de sessão real nunca poder ser reaproveitado como
    // se fosse um pendente (ex.: alguém trocando o nome do cookie na mão).
    public String generateMfaPendingToken(User user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("auth-api")
                    .withSubject(user.getEmail())
                    .withClaim("mfaPending", true)
                    .withExpiresAt(Instant.now().plusSeconds(5 * 60))
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro ao gerar token de MFA pendente", exception);
        }
    }

    /** Retorna o e-mail do subject só se o token for válido E carregar a claim mfaPending=true;
     * caso contrário "" (mesmo contrato de erro de validateToken). */
    public String validateMfaPendingToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            var decoded = JWT.require(algorithm)
                    .withIssuer("auth-api")
                    .withClaim("mfaPending", true)
                    .build()
                    .verify(token);
            return decoded.getSubject();
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