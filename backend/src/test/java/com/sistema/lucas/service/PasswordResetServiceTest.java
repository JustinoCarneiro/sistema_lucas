package com.sistema.lucas.service;

import com.sistema.lucas.model.PasswordResetToken;
import com.sistema.lucas.model.User;
import com.sistema.lucas.repository.PasswordResetTokenRepository;
import com.sistema.lucas.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class PasswordResetServiceTest {

    @InjectMocks private PasswordResetService passwordResetService;

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private EmailService emailService;
    @Mock private PasswordEncoder passwordEncoder;

    @org.junit.jupiter.api.BeforeEach
    void setup() {
        ReflectionTestUtils.setField(passwordResetService, "frontendUrl", "http://localhost:4200");
    }

    // ──────────────────────── solicitarRecuperacao ────────────────────────

    @Nested @DisplayName("solicitarRecuperacao")
    class SolicitarRecuperacaoTests {

        @Test @DisplayName("Deve gerar token e enviar e-mail quando usuário existe")
        void deveGerarTokenEEnviarEmail() {
            var user = new User(); user.setId(1L); user.setEmail("user@test.com"); user.setName("Lucas");
            when(userRepository.findByEmail("user@test.com")).thenReturn(user);

            passwordResetService.solicitarRecuperacao("user@test.com");

            var captor = ArgumentCaptor.forClass(PasswordResetToken.class);
            verify(tokenRepository).save(captor.capture());
            assertNotNull(captor.getValue().getToken());
            assertFalse(captor.getValue().isUsado());
            assertTrue(captor.getValue().getExpiracao().isAfter(LocalDateTime.now()));

            verify(emailService).enviar(eq("user@test.com"), anyString(), anyString());
        }

        @Test @DisplayName("Não deve lançar exceção quando e-mail não existe (segurança — não revela existência)")
        void naoLancaExcecaoParaEmailInexistente() {
            when(userRepository.findByEmail("naoexiste@test.com")).thenReturn(null);

            assertDoesNotThrow(() -> passwordResetService.solicitarRecuperacao("naoexiste@test.com"));
            verifyNoInteractions(emailService);
        }

        @Test @DisplayName("Deve apagar tokens anteriores do mesmo usuário antes de gerar novo")
        void deveApagarTokensAnteriores() {
            var user = new User(); user.setId(5L); user.setEmail("user@test.com"); user.setName("Teste");
            when(userRepository.findByEmail("user@test.com")).thenReturn(user);

            passwordResetService.solicitarRecuperacao("user@test.com");

            verify(tokenRepository).deleteByUserId(5L);
        }
    }

    // ──────────────────────── redefinirSenha ────────────────────────

    @Nested @DisplayName("redefinirSenha")
    class RedefinirSenhaTests {

        private PasswordResetToken tokenValido() {
            var user = new User(); user.setId(1L);
            var t = new PasswordResetToken();
            t.setToken("token-valido"); t.setUser(user); t.setUsado(false);
            t.setExpiracao(LocalDateTime.now().plusHours(1));
            return t;
        }

        @Test @DisplayName("Deve redefinir senha com sucesso para token válido")
        void deveRedefinirSenhaComSucesso() {
            var token = tokenValido();
            when(tokenRepository.findByToken("token-valido")).thenReturn(Optional.of(token));
            when(passwordEncoder.encode("novaSenha123")).thenReturn("hash-nova-senha");

            assertDoesNotThrow(() -> passwordResetService.redefinirSenha("token-valido", "novaSenha123"));

            assertEquals("hash-nova-senha", token.getUser().getPassword());
            assertTrue(token.isUsado());
            verify(userRepository).save(token.getUser());
        }

        @Test @DisplayName("Deve lançar exceção para token inválido (não encontrado)")
        void lancaExcecaoParaTokenInvalido() {
            when(tokenRepository.findByToken("token-invalido")).thenReturn(Optional.empty());

            var ex = assertThrows(RuntimeException.class, () ->
                passwordResetService.redefinirSenha("token-invalido", "senha"));

            assertTrue(ex.getMessage().contains("inválido ou expirado"));
        }

        @Test @DisplayName("Deve lançar exceção para token já utilizado")
        void lancaExcecaoParaTokenJaUsado() {
            var token = tokenValido(); token.setUsado(true);
            when(tokenRepository.findByToken("token-usado")).thenReturn(Optional.of(token));

            var ex = assertThrows(RuntimeException.class, () ->
                passwordResetService.redefinirSenha("token-usado", "senha"));

            assertTrue(ex.getMessage().contains("já foi utilizado"));
        }

        @Test @DisplayName("Deve lançar exceção para token expirado")
        void lancaExcecaoParaTokenExpirado() {
            var token = tokenValido();
            token.setExpiracao(LocalDateTime.now().minusHours(1)); // expirado
            when(tokenRepository.findByToken("token-expirado")).thenReturn(Optional.of(token));

            var ex = assertThrows(RuntimeException.class, () ->
                passwordResetService.redefinirSenha("token-expirado", "senha"));

            assertTrue(ex.getMessage().contains("expirou"));
        }
    }
}
