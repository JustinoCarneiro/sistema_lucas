package com.sistema.lucas.service;

import com.sistema.lucas.repository.PasswordResetTokenRepository;
import com.sistema.lucas.repository.RefreshTokenRepository;
import com.sistema.lucas.repository.TokenDenylistRepository;
import com.sistema.lucas.repository.VerificationTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * AUD-07 (LGPD Art. 6º, III — Princípio da Necessidade):
 * Rotina automatizada de expurgo de dados que já cumpriram a sua finalidade.
 * Executa diariamente às 03:00 para minimizar impacto no sistema.
 */
@Component
public class DataCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(DataCleanupTask.class);

    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenDenylistRepository tokenDenylistRepository;
    private final AuditLogService auditLogService;

    public DataCleanupTask(VerificationTokenRepository verificationTokenRepository,
                           PasswordResetTokenRepository passwordResetTokenRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           TokenDenylistRepository tokenDenylistRepository,
                           AuditLogService auditLogService) {
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenDenylistRepository = tokenDenylistRepository;
        this.auditLogService = auditLogService;
    }

    @Scheduled(cron = "0 0 3 * * *") // Diariamente às 03:00
    @Transactional
    public void expurgarTokensExpirados() {
        LocalDateTime agora = LocalDateTime.now();
        log.info("[LGPD] Iniciando expurgo automático de tokens expirados...");

        // 1. Tokens de verificação de e-mail expirados (validade 24h)
        long verificacaoRemovidos = verificationTokenRepository.deleteByExpiryDateBefore(agora);

        // 2. Tokens de redefinição de senha expirados OU já utilizados
        long resetExpirados = passwordResetTokenRepository.deleteByExpiracaoBefore(agora);
        long resetUsados = passwordResetTokenRepository.deleteByUsadoTrue();

        // 3. SEC-03: Refresh Tokens expirados, já usados ou revogados
        long refreshRemovidos = refreshTokenRepository.deleteByExpiresAtBeforeOrUsedTrueOrRevokedAtNotNull(agora);

        // 4. SEC-03: Access Tokens expirados na Denylist
        long denylistRemovidos = tokenDenylistRepository.deleteByExpiresAtBefore(agora);

        long total = verificacaoRemovidos + resetExpirados + resetUsados + refreshRemovidos + denylistRemovidos;

        if (total > 0) {
            String detalhes = String.format(
                "Verificação: %d | Reset expirados: %d | Reset usados: %d | Refresh Tokens: %d | Denylist: %d",
                verificacaoRemovidos, resetExpirados, resetUsados, refreshRemovidos, denylistRemovidos
            );
            log.info("[LGPD] Expurgo concluído: {} tokens removidos ({})", total, detalhes);
            auditLogService.log("SISTEMA", "EXPURGO_LGPD", "Tokens", null, detalhes);
        } else {
            log.info("[LGPD] Expurgo concluído: nenhum token expirado encontrado.");
        }
    }
}
