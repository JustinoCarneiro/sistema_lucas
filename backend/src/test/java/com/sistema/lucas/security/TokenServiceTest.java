package com.sistema.lucas.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import com.sistema.lucas.security.service.TokenService;

class TokenServiceTest {

    private TokenService tokenService;

    @BeforeEach
    void setup() {
        tokenService = new TokenService();
        // Injeta o segredo manualmente pois o @Value não funciona em teste unitário puro
        ReflectionTestUtils.setField(tokenService, "secret", "my-secret-key-123");
    }

    @Test
    @DisplayName("Deve gerar um token válido e conseguir extrair o email dele")
    void gerarValidarToken() {
        String email = "admin@clinica.com";
        
        // Gera o token
        String token = tokenService.generateToken(email);
        
        assertNotNull(token);
        assertFalse(token.isEmpty());

        // Valida o token e extrai o subject (email)
        String subject = tokenService.validateToken(token);
        
        assertEquals(email, subject);
    }

    @Test
    @DisplayName("Deve retornar string vazia para token inválido")
    void tokenInvalido() {
        String subject = tokenService.validateToken("token-inventado-errado");
        assertEquals("", subject);
    }
}