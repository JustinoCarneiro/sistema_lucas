// backend/src/main/java/com/sistema/lucas/security/service/MfaService.java
package com.sistema.lucas.security.service;

import com.sistema.lucas.model.User;
import com.sistema.lucas.repository.RefreshTokenRepository;
import com.sistema.lucas.repository.UserRepository;
import com.sistema.lucas.security.dto.MfaSetupResponseDTO;
import com.sistema.lucas.service.AuditLogService;
import com.sistema.lucas.service.MfaBackupCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MfaService {

    @Autowired private UserRepository userRepository;
    @Autowired private TotpService totpService;
    @Autowired private MfaBackupCodeService mfaBackupCodeService;
    @Autowired private AuditLogService auditLogService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private TokenService tokenService;

    public MfaSetupResponseDTO iniciarSetup(User user) {
        String secret = totpService.generateSecretBase32();
        // mfaEnabled continua false até /enable confirmar com um código válido — se o usuário
        // abandonar o setup, sobra um secret sem uso, sem efeito funcional nenhum.
        user.setTotpSecret(secret);
        userRepository.save(user);

        String uri = totpService.buildOtpAuthUri(secret, user.getEmail());
        auditLogService.log(user.getEmail(), "MFA_SETUP_INICIADO", "User", user.getId(), "Usuário iniciou configuração de MFA");
        return new MfaSetupResponseDTO(secret, uri);
    }

    @Transactional
    public List<String> ativar(User user, String code) {
        if (user.getTotpSecret() == null) {
            throw new RuntimeException("Nenhum setup de MFA em andamento. Chame /auth/mfa/setup primeiro.");
        }
        if (!totpService.verifyCode(user.getTotpSecret(), code)) {
            auditLogService.log(user.getEmail(), "MFA_ATIVACAO_FALHOU", "User", user.getId(), "Código TOTP inválido ao tentar ativar MFA");
            throw new RuntimeException("Código inválido.");
        }

        user.setMfaEnabled(true);
        userRepository.save(user);

        List<String> backupCodes = mfaBackupCodeService.gerarNovos(user);
        auditLogService.log(user.getEmail(), "MFA_ATIVADO", "User", user.getId(), "Usuário ativou MFA (TOTP)");
        return backupCodes;
    }

    @Transactional
    public void desativar(User user, String password, String code) {
        if (!passwordEncoder.matches(password, user.getPassword())) {
            auditLogService.log(user.getEmail(), "MFA_DESATIVACAO_FALHOU", "User", user.getId(), "Senha incorreta ao tentar desativar MFA");
            throw new RuntimeException("Senha incorreta.");
        }
        boolean codigoValido = totpService.verifyCode(user.getTotpSecret(), code)
            || mfaBackupCodeService.consumirSeValido(user, code);
        if (!codigoValido) {
            auditLogService.log(user.getEmail(), "MFA_DESATIVACAO_FALHOU", "User", user.getId(), "Código inválido ao tentar desativar MFA");
            throw new RuntimeException("Código inválido.");
        }

        user.setMfaEnabled(false);
        user.setTotpSecret(null);
        userRepository.save(user);
        mfaBackupCodeService.apagarTodos(user);
        refreshTokenRepository.deleteByUser(user);

        auditLogService.log(user.getEmail(), "MFA_DESATIVADO", "User", user.getId(),
            "Usuário desativou MFA — todas as sessões (refresh tokens) revogadas");
    }

    /** Confere o token de MFA pendente (cookie) + o código (TOTP ou backup) — devolve o User
     * autenticado se tudo bater, ou lança se qualquer etapa falhar. */
    public User verificarLogin(String pendingToken, String code) {
        String email = tokenService.validateMfaPendingToken(pendingToken);
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Sessão de verificação expirada. Faça login novamente.");
        }

        User user = userRepository.findByEmail(email);
        if (user == null || !user.isMfaEnabled()) {
            throw new RuntimeException("Sessão de verificação inválida. Faça login novamente.");
        }

        boolean codigoValido = totpService.verifyCode(user.getTotpSecret(), code)
            || mfaBackupCodeService.consumirSeValido(user, code);
        if (!codigoValido) {
            auditLogService.log(email, "MFA_VERIFICACAO_FALHOU", "User", user.getId(), "Código de MFA inválido no login");
            throw new RuntimeException("Código inválido.");
        }

        auditLogService.log(email, "MFA_VERIFICACAO_SUCESSO", "User", user.getId(), "Login concluído com MFA");
        return user;
    }
}
