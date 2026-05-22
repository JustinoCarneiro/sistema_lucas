package com.sistema.lucas.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String remetente;

    @Value("${app.mail.display-name:Instituto Lucas}")
    private String nomeRemetente;

    // Envio assíncrono — não bloqueia a requisição do usuário
    @Async
    public void enviar(String destinatario, String assunto, String corpoHtml) {
        try {
            MimeMessage mensagem = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensagem, true, "UTF-8");

            helper.setFrom(java.util.Objects.requireNonNull(remetente), java.util.Objects.requireNonNull(nomeRemetente));
            helper.setReplyTo(java.util.Objects.requireNonNull(remetente), java.util.Objects.requireNonNull(nomeRemetente));
            helper.setTo(java.util.Objects.requireNonNull(destinatario));
            helper.setSubject(java.util.Objects.requireNonNull(assunto));
            helper.setText(java.util.Objects.requireNonNull(corpoHtml), true);

            mailSender.send(mensagem);
        } catch (MessagingException | UnsupportedEncodingException e) {
            System.err.println("⚠️ Erro ao enviar e-mail para " + destinatario + ": " + e.getMessage());
        }
    }
}