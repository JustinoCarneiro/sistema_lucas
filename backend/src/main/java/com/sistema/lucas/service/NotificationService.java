package com.sistema.lucas.service;

import com.sistema.lucas.config.WhatsAppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;
    private final WhatsAppProperties whatsAppProperties;
    private final RestTemplate restTemplate = new RestTemplate(); // Helper para chamadas HTTP

    @Async
    public void sendAppointmentConfirmation(String to, String patientName, String doctorName, String dateTime) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("nao-responda@sistemalucas.com");
            message.setTo(to);
            message.setSubject("Consulta Confirmada - Sistema Lucas");
            message.setText("Olá " + patientName + "!\n\n" +
                    "Sua consulta com o(a) Dr(a). " + doctorName + " foi agendada com sucesso para: " + dateTime + ".\n\n" +
                    "Te esperamos na clínica!");
            
            mailSender.send(message);
            System.out.println("📧 E-mail de confirmação enviado para: " + to);
        } catch (Exception e) {
            System.err.println("❌ Erro ao enviar e-mail: " + e.getMessage());
        }
    }

    @Async
    public void sendWhatsAppMessage(String number, String message) {
        try {
            // 1. Limpar o número (manter apenas dígitos)
            String cleanNumber = number.replaceAll("\\D", "");
            
            // 2. Garantir código do país (ex: 55 para Brasil)
            if (!cleanNumber.startsWith("55") && cleanNumber.length() >= 10) {
                cleanNumber = "55" + cleanNumber;
            }

            // 3. Montar a URL (Padrão Evolution API)
            String url = String.format("%s/message/sendText/%s", whatsAppProperties.apiUrl(), whatsAppProperties.instanceName());

            // 4. Montar o corpo da requisição
            Map<String, Object> body = Map.of(
                "number", cleanNumber,
                "text", message
            );

            // 5. Configurar Headers de segurança
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", whatsAppProperties.apiKey());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            // 6. Enviar a requisição POST
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("📱 WhatsApp enviado com sucesso para: " + cleanNumber);
            }
        } catch (Exception e) {
            // Como é @Async, o erro não quebra o fluxo do agendamento, apenas avisa no console
            System.err.println("📱 ❌ Falha ao enviar WhatsApp Real: " + e.getMessage());
        }
    }

    @Async
    public void sendGenericEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            System.out.println("📧 Notificação enviada para: " + to);
        } catch (Exception e) {
            System.err.println("❌ Erro ao enviar e-mail genérico: " + e.getMessage());
        }
    }
}