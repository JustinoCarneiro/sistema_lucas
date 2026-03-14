// backend/src/main/java/com/sistema/lucas/service/LembreteScheduler.java
package com.sistema.lucas.service;

import com.sistema.lucas.repository.AppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class LembreteScheduler {

    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private EmailTemplateService emailTemplateService;

    // Executa todo dia às 10h da manhã
    // Busca consultas do dia seguinte e envia lembrete
    @Scheduled(cron = "0 0 10 * * *", zone = "America/Sao_Paulo")
    public void enviarLembretes() {
        LocalDateTime inicioDAmanha = LocalDateTime.now().plusDays(1).toLocalDate().atStartOfDay();
        LocalDateTime fimDAmanha = inicioDAmanha.plusDays(1);

        var consultas = appointmentRepository.findConsultasParaLembrete(inicioDAmanha, fimDAmanha);

        consultas.forEach(consulta -> {
            try {
                emailTemplateService.enviarLembrete(consulta);
                System.out.println("📧 Lembrete enviado para: " + consulta.getPatient().getEmail());
            } catch (Exception e) {
                System.err.println("⚠️ Falha ao enviar lembrete: " + e.getMessage());
            }
        });

        System.out.println("✅ Job de lembretes concluído. " + consultas.size() + " e-mail(s) enviado(s).");
    }
}