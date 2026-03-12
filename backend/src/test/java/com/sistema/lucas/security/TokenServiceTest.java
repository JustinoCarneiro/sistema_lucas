package com.sistema.lucas.security;

import com.sistema.lucas.model.User;
import com.sistema.lucas.model.enums.Role;
import com.sistema.lucas.security.service.TokenService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

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
        // 1. Cria um utilizador fictício com Email e Role (pois o TokenService agora exige isso)
        User fakeUser = new User();
        fakeUser.setEmail("admin@clinica.com");
        fakeUser.setRole(Role.ADMIN);
        
        // 2. Passa o objeto User para o gerador
        String token = tokenService.generateToken(fakeUser);
        
        assertNotNull(token);
        assertFalse(token.isEmpty());

        // 3. Valida o token e extrai o subject (email)
        String subject = tokenService.validateToken(token);
        
        assertEquals(fakeUser.getEmail(), subject);
    }

    @Test
    @DisplayName("Deve retornar string vazia para token inválido")
    void tokenInvalido() {
        String subject = tokenService.validateToken("token-inventado-errado");
        assertEquals("", subject);
    }
}