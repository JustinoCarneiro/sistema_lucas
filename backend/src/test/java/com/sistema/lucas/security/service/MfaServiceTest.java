package com.sistema.lucas.security.service;

import com.sistema.lucas.model.User;
import com.sistema.lucas.repository.RefreshTokenRepository;
import com.sistema.lucas.repository.UserRepository;
import com.sistema.lucas.service.AuditLogService;
import com.sistema.lucas.service.MfaBackupCodeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class MfaServiceTest {

    @InjectMocks private MfaService mfaService;

    @Mock private UserRepository userRepository;
    @Mock private TotpService totpService;
    @Mock private MfaBackupCodeService mfaBackupCodeService;
    @Mock private AuditLogService auditLogService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private TokenService tokenService;

    private User usuario() {
        var u = new User();
        u.setId(1L);
        u.setEmail("user@test.com");
        u.setPassword("hash-da-senha");
        return u;
    }

    @Nested @DisplayName("iniciarSetup")
    class SetupTests {
        @Test @DisplayName("Gera secret novo, persiste (mfaEnabled continua false) e devolve URI")
        void geraSecretEDevolveUri() {
            var user = usuario();
            when(totpService.generateSecretBase32()).thenReturn("SECRETBASE32");
            when(totpService.buildOtpAuthUri("SECRETBASE32", "user@test.com")).thenReturn("otpauth://totp/x");

            var result = mfaService.iniciarSetup(user);

            assertEquals("SECRETBASE32", result.secretBase32());
            assertEquals("otpauth://totp/x", result.otpAuthUri());
            assertEquals("SECRETBASE32", user.getTotpSecret());
            assertFalse(user.isMfaEnabled());
            verify(userRepository).save(user);
        }
    }

    @Nested @DisplayName("ativar")
    class AtivarTests {
        @Test @DisplayName("Código válido liga mfaEnabled e devolve backup codes")
        void codigoValidoAtiva() {
            var user = usuario();
            user.setTotpSecret("SECRET");
            when(totpService.verifyCode("SECRET", "123456")).thenReturn(true);
            when(mfaBackupCodeService.gerarNovos(user)).thenReturn(List.of("A", "B", "C"));

            var codes = mfaService.ativar(user, "123456");

            assertEquals(List.of("A", "B", "C"), codes);
            assertTrue(user.isMfaEnabled());
            verify(userRepository).save(user);
        }

        @Test @DisplayName("Código inválido lança exceção e não ativa")
        void codigoInvalidoNaoAtiva() {
            var user = usuario();
            user.setTotpSecret("SECRET");
            when(totpService.verifyCode("SECRET", "000000")).thenReturn(false);

            var ex = assertThrows(RuntimeException.class, () -> mfaService.ativar(user, "000000"));
            assertTrue(ex.getMessage().contains("inválido"));
            assertFalse(user.isMfaEnabled());
            verify(mfaBackupCodeService, never()).gerarNovos(any());
        }

        @Test @DisplayName("Sem setup em andamento (sem secret) lança exceção")
        void semSecretLancaExcecao() {
            var user = usuario();
            var ex = assertThrows(RuntimeException.class, () -> mfaService.ativar(user, "123456"));
            assertTrue(ex.getMessage().contains("setup"));
        }
    }

    @Nested @DisplayName("desativar")
    class DesativarTests {
        @Test @DisplayName("Senha e código válidos desativam e revogam todos os refresh tokens")
        void senhaECodigoValidosDesativam() {
            var user = usuario();
            user.setTotpSecret("SECRET");
            user.setMfaEnabled(true);
            when(passwordEncoder.matches("senha123", "hash-da-senha")).thenReturn(true);
            when(totpService.verifyCode("SECRET", "123456")).thenReturn(true);

            mfaService.desativar(user, "senha123", "123456");

            assertFalse(user.isMfaEnabled());
            assertNull(user.getTotpSecret());
            verify(mfaBackupCodeService).apagarTodos(user);
            verify(refreshTokenRepository).deleteByUser(user);
        }

        @Test @DisplayName("Senha incorreta lança exceção e não desativa")
        void senhaIncorretaLancaExcecao() {
            var user = usuario();
            user.setMfaEnabled(true);
            when(passwordEncoder.matches("senha-errada", "hash-da-senha")).thenReturn(false);

            var ex = assertThrows(RuntimeException.class, () -> mfaService.desativar(user, "senha-errada", "123456"));
            assertTrue(ex.getMessage().contains("Senha incorreta"));
            assertTrue(user.isMfaEnabled());
            verify(refreshTokenRepository, never()).deleteByUser(any());
        }

        @Test @DisplayName("Aceita backup code no lugar do TOTP")
        void aceitaBackupCodeNoLugarDoTotp() {
            var user = usuario();
            user.setTotpSecret("SECRET");
            user.setMfaEnabled(true);
            when(passwordEncoder.matches("senha123", "hash-da-senha")).thenReturn(true);
            when(totpService.verifyCode("SECRET", "BACKUP-CODE")).thenReturn(false);
            when(mfaBackupCodeService.consumirSeValido(user, "BACKUP-CODE")).thenReturn(true);

            mfaService.desativar(user, "senha123", "BACKUP-CODE");

            assertFalse(user.isMfaEnabled());
        }
    }

    @Nested @DisplayName("verificarLogin")
    class VerificarLoginTests {
        @Test @DisplayName("Pending token e código válidos devolvem o usuário")
        void pendingTokenECodigoValidosDevolvemUsuario() {
            var user = usuario();
            user.setTotpSecret("SECRET");
            user.setMfaEnabled(true);
            when(tokenService.validateMfaPendingToken("PENDING")).thenReturn("user@test.com");
            when(userRepository.findByEmail("user@test.com")).thenReturn(user);
            when(totpService.verifyCode("SECRET", "123456")).thenReturn(true);

            var result = mfaService.verificarLogin("PENDING", "123456");

            assertEquals(user, result);
        }

        @Test @DisplayName("Pending token inválido/expirado lança exceção")
        void pendingTokenInvalidoLancaExcecao() {
            when(tokenService.validateMfaPendingToken("EXPIRADO")).thenReturn("");

            var ex = assertThrows(RuntimeException.class, () -> mfaService.verificarLogin("EXPIRADO", "123456"));
            assertTrue(ex.getMessage().contains("expirada") || ex.getMessage().contains("inválida"));
            verifyNoInteractions(userRepository);
        }

        @Test @DisplayName("Código incorreto lança exceção")
        void codigoIncorretoLancaExcecao() {
            var user = usuario();
            user.setTotpSecret("SECRET");
            user.setMfaEnabled(true);
            when(tokenService.validateMfaPendingToken("PENDING")).thenReturn("user@test.com");
            when(userRepository.findByEmail("user@test.com")).thenReturn(user);
            when(totpService.verifyCode("SECRET", "000000")).thenReturn(false);
            when(mfaBackupCodeService.consumirSeValido(user, "000000")).thenReturn(false);

            assertThrows(RuntimeException.class, () -> mfaService.verificarLogin("PENDING", "000000"));
        }

        @Test @DisplayName("Usuário sem MFA mais ativo (desativado entre o login e o verify) lança exceção")
        void usuarioSemMfaAtivoLancaExcecao() {
            var user = usuario();
            user.setMfaEnabled(false);
            when(tokenService.validateMfaPendingToken("PENDING")).thenReturn("user@test.com");
            when(userRepository.findByEmail("user@test.com")).thenReturn(user);

            assertThrows(RuntimeException.class, () -> mfaService.verificarLogin("PENDING", "123456"));
        }
    }
}
