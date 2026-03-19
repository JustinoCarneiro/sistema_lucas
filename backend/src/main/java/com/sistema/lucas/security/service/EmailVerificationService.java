package com.sistema.lucas.security.service;

import com.sistema.lucas.model.User;
import com.sistema.lucas.model.VerificationToken;
import com.sistema.lucas.repository.UserRepository;
import com.sistema.lucas.repository.VerificationTokenRepository;
import com.sistema.lucas.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class EmailVerificationService {

    @Autowired
    private VerificationTokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Value("${FRONTEND_URL}")
    private String frontendUrl;

    public void createAndSendVerificationEmail(User user) {
        // Remove token anterior se existir
        tokenRepository.findByUser(user).ifPresent(tokenRepository::delete);

        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(token, user);
        tokenRepository.save(verificationToken);

        String verifyUrl = frontendUrl + "/verify-email?token=" + token;
        
        String assunto = "Verifique seu e-mail — Sistema Lucas";
        String corpo = "<h1>Olá " + user.getName() + "!</h1>" +
                "<p>Obrigado por se cadastrar no Sistema Lucas. Por favor, clique no link abaixo para verificar seu e-mail:</p>" +
                "<p><a href=\"" + verifyUrl + "\" style=\"background-color: #1e3a8a; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; font-weight: bold;\">Verificar E-mail</a></p>" +
                "<p>Se o botão não funcionar, copie e cole este link no seu navegador: <br>" + verifyUrl + "</p>" +
                "<br><p>Atenciosamente,<br>Equipe Sistema Lucas</p>";

        emailService.enviar(user.getEmail(), assunto, corpo);
    }

    @Transactional
    public String verifyToken(String tokenValue) {
        Optional<VerificationToken> tokenOpt = tokenRepository.findByToken(tokenValue);
        
        if (tokenOpt.isEmpty()) {
            return "Token inválido";
        }

        VerificationToken token = tokenOpt.get();
        if (token.isExpired()) {
            return "Token expirado";
        }

        User user = token.getUser();
        user.setVerified(true);
        userRepository.save(user);
        tokenRepository.delete(token);

        return "E-mail verificado com sucesso!";
    }
}
