package com.sistema.lucas.service;

import com.sistema.lucas.model.Professional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AvailabilityNotificationTask {

    private static final Logger logger = LoggerFactory.getLogger(AvailabilityNotificationTask.class);

    @Autowired
    private AvailabilityService availabilityService;

    @Autowired
    private EmailTemplateService emailTemplateService;

    /**
     * Roda diariamente às 09:00.
     * Envia lembretes nos dias 20, 23 e diariamente nos últimos 5 dias do mês.
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void gerenciarNotificacoesAgenda() {
        LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        int dia = hoje.getDayOfMonth();
        LocalDate fimDoMes = hoje.withDayOfMonth(hoje.lengthOfMonth());
        long diasParaAcabar = ChronoUnit.DAYS.between(hoje, fimDoMes);

        boolean deveNotificar = (dia == 20 || dia == 23 || diasParaAcabar < 5);

        if (!deveNotificar) return;

        YearMonth proximoMes = YearMonth.from(hoje).plusMonths(1);
        logger.info("Verificando pendências de agenda para {}. Dias restantes no mês atual: {}", proximoMes, diasParaAcabar);
        
        List<Professional> profissionaisPendentes = availabilityService.getProfissionaisSemAgendaNoMes(proximoMes);
        
        if (profissionaisPendentes.isEmpty()) {
            logger.info("Todos os profissionais já preencheram a agenda.");
            return;
        }

        logger.info("Enviando lembretes para {} profissionais.", profissionaisPendentes.size());

        for (Professional prof : profissionaisPendentes) {
            try {
                // Se faltar menos de 5 dias, o e-mail ganha um tom mais urgente.
                emailTemplateService.enviarLembreteSubmissaoAgenda(prof, proximoMes, diasParaAcabar < 5);
            } catch (Exception e) {
                logger.error("Erro ao enviar lembrete para {}: {}", prof.getEmail(), e.getMessage());
            }
        }
    }
}
