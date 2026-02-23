package com.sistema.lucas.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.sistema.lucas.domain.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class TokenService {

    // Lê a senha secreta do application.properties. Se não achar, usa o valor default.
    @Value("${api.security.token.secret:minha-senha-super-secreta-123}")
    private String secret;

    // 1. GERA O TOKEN (Na hora do Login)
    public String generateToken(User user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("sistema-lucas")
                    .withSubject(user.getEmail())
                    .withClaim("role", user.getRole().name()) // <--- ✨ ADICIONE ESTA LINHA AQUI! ✨
                    .withExpiresAt(generateExpirationDate())
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro ao gerar token JWT", exception);
        }
    }

    // 2. VALIDA O TOKEN (Em todas as outras requisições da API)
    public String validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("sistema-lucas")
                    .build()
                    .verify(token) // Se o token for falso ou expirado, ele quebra aqui
                    .getSubject(); // Devolve o email que estava salvo lá dentro
        } catch (JWTVerificationException exception) {
            return ""; // Retorna vazio se der erro, o Spring Security bloqueia o acesso
        }
    }

    // Regra de Negócio: O token vale por 2 horas
    private Instant generateExpirationDate() {
        return LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-03:00"));
    }
}