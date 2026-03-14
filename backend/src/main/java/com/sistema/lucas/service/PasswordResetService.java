// backend/src/main/java/com/sistema/lucas/service/PasswordResetService.java
package com.sistema.lucas.service;

import com.sistema.lucas.model.PasswordResetToken;
import com.sistema.lucas.repository.PasswordResetTokenRepository;
import com.sistema.lucas.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PasswordResetService {

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordResetTokenRepository tokenRepository;
    @Autowired private EmailService emailService;
    @Autowired private PasswordEncoder passwordEncoder;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    // ─── Solicitar recuperação ────────────────────────────────────────────────

    @Transactional
    public void solicitarRecuperacao(String email) {
        var user = userRepository.findByEmail(email);

        // Não revelamos se o e-mail existe ou não (segurança)
        if (user == null) return;

        // Remove tokens anteriores do mesmo usuário
        tokenRepository.deleteByUserId(user.getId());

        // Gera token único
        String token = UUID.randomUUID().toString();

        var resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiracao(LocalDateTime.now().plusHours(24));
        resetToken.setUsado(false);
        tokenRepository.save(resetToken);

        // Envia o e-mail
        String link = frontendUrl + "/redefinir-senha?token=" + token;
        enviarEmailRecuperacao(email, user.getName(), link);
    }

    // ─── Redefinir senha ──────────────────────────────────────────────────────

    @Transactional
    public void redefinirSenha(String token, String novaSenha) {
        var resetToken = tokenRepository.findByToken(token)
            .orElseThrow(() -> new RuntimeException("Token inválido ou expirado."));

        if (resetToken.isUsado()) {
            throw new RuntimeException("Este link já foi utilizado.");
        }

        if (resetToken.estaExpirado()) {
            throw new RuntimeException("Este link expirou. Solicite um novo.");
        }

        var user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(novaSenha));
        userRepository.save(user);

        resetToken.setUsado(true);
        tokenRepository.save(resetToken);
    }

    // ─── E-mail de recuperação ────────────────────────────────────────────────

    private void enviarEmailRecuperacao(String email, String nome, String link) {
        String html = """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background:#f4f4f4;font-family:Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f4f4;padding:40px 0;">
                <tr><td align="center">
                  <table width="560" cellpadding="0" cellspacing="0"
                    style="background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.08);">

                    <tr>
                      <td style="background:#1e3a5f;padding:28px 32px;">
                        <h1 style="margin:0;color:#ffffff;font-size:20px;font-weight:700;">Recuperação de senha</h1>
                        <p style="margin:4px 0 0;color:rgba(255,255,255,0.8);font-size:13px;">Sistema Lucas — Gestão Clínica</p>
                      </td>
                    </tr>

                    <tr>
                      <td style="padding:28px 32px;">
                        <p style="margin:0 0 12px;font-size:16px;color:#222;">Olá, %s!</p>
                        <p style="margin:0 0 24px;font-size:14px;color:#555;">
                          Recebemos uma solicitação para redefinir a senha da sua conta.
                          Clique no botão abaixo para criar uma nova senha.
                        </p>

                        <div style="text-align:center;margin:24px 0;">
                          <a href="%s"
                            style="background:#1e3a5f;color:#ffffff;padding:14px 32px;border-radius:8px;
                                   text-decoration:none;font-size:15px;font-weight:700;display:inline-block;">
                            Redefinir minha senha
                          </a>
                        </div>

                        <p style="margin:20px 0 0;font-size:13px;color:#777;">
                          Este link é válido por 24 horas. Se você não solicitou a recuperação de senha,
                          ignore este e-mail.
                        </p>
                      </td>
                    </tr>

                    <tr>
                      <td style="padding:16px 32px;background:#f9f9f9;border-top:1px solid #ececec;">
                        <p style="margin:0;font-size:12px;color:#aaa;text-align:center;">
                          Este é um e-mail automático. Por favor, não responda.
                        </p>
                      </td>
                    </tr>

                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(nome, link);

        emailService.enviar(email, "Recuperação de senha — Sistema Lucas", html);
    }
}