package com.sistema.lucas.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String remetente;

    // Envio assíncrono — não bloqueia a requisição do usuário
    @Async
    public void enviar(String destinatario, String assunto, String corpoHtml) {
        try {
            MimeMessage mensagem = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensagem, true, "UTF-8");

            helper.setFrom(java.util.Objects.requireNonNull(remetente));
            helper.setTo(java.util.Objects.requireNonNull(destinatario));
            helper.setSubject(java.util.Objects.requireNonNull(assunto));
            helper.setText(java.util.Objects.requireNonNull(corpoHtml), true); // true = HTML

            mailSender.send(mensagem);
        } catch (MessagingException e) {
            // Log do erro sem derrubar o fluxo principal
            System.err.println("⚠️ Erro ao enviar e-mail para " + destinatario + ": " + e.getMessage());
        }
    }
}