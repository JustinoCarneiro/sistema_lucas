package com.sistema.lucas.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

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
            log.error("Erro ao montar e-mail para {}: {}", destinatario, e.getMessage());
        } catch (MailException e) {
            // mailSender.send() lança MailException (unchecked, ex.: MailSendException se o
            // SMTP recusar a conexão) — não é subtipo de MessagingException, ficava sem log.
            log.error("Erro ao enviar e-mail para {} (falha de SMTP): {}", destinatario, e.getMessage());
        }
    }
}