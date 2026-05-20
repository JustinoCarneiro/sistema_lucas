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
    @Autowired private com.sistema.lucas.repository.ProfessionalRepository professionalRepository;
    @Autowired private com.sistema.lucas.repository.ProfessionalAvailabilityRepository availabilityRepository;
    @Autowired private EmailService emailService;

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

    // Executa todo dia às 8h da manhã
    @Scheduled(cron = "0 0 8 * * *", zone = "America/Sao_Paulo")
    public void notificarPrazoAgenda() {
        java.time.LocalDate hoje = java.time.LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo"));
        java.time.YearMonth mesAtual = java.time.YearMonth.from(hoje);
        java.time.LocalDate fimDoMes = mesAtual.atEndOfMonth();
        long diasParaFim = java.time.temporal.ChronoUnit.DAYS.between(hoje, fimDoMes);

        // Se bloqueia quando < 5 dias pro fim (ex: dia 27 em um mês de 31 dias).
        // 5 dias antes desse limite significa faltar 10 dias pro fim (ex: dia 21).
        if (diasParaFim == 10) {
            java.time.YearMonth proximoMes = mesAtual.plusMonths(1);
            var todos = professionalRepository.findAll();
            for (var p : todos) {
                boolean enviou = availabilityRepository.existsByProfessionalIdAndDateBetween(
                    p.getId(), proximoMes.atDay(1), proximoMes.atEndOfMonth()
                );
                if (!enviou) {
                    emailService.enviar(
                        p.getEmail(),
                        "Lembrete: Prazo da Agenda do Próximo Mês",
                        "<p>Olá, " + p.getName() + ",</p><p>Faltam exatos 5 dias para o limite de submissão da sua agenda de " + proximoMes + ".</p><p>Por favor, acesse o sistema e envie seus horários.</p>"
                    );
                }
            }
        }
    }

    // Executa toda segunda-feira às 9h da manhã para alertar sobre pendências
    @Scheduled(cron = "0 0 9 * * MON", zone = "America/Sao_Paulo")
    public void notificarPendenciasSemanais() {
        // Para profissionais: status AGUARDANDO_CONFIRMACAO
        var pendentesProfissional = appointmentRepository.findPendentesNoFuturo(com.sistema.lucas.model.enums.StatusConsulta.AGUARDANDO_CONFIRMACAO);
        java.util.Map<com.sistema.lucas.model.Professional, Long> qtdeProf = pendentesProfissional.stream()
            .collect(java.util.stream.Collectors.groupingBy(com.sistema.lucas.model.Appointment::getProfessional, java.util.stream.Collectors.counting()));
        
        qtdeProf.forEach((prof, qtde) -> {
            try {
                emailTemplateService.enviarLembreteSemanalPendenciasProfissional(prof, qtde);
                System.out.println("📧 Lembrete semanal enviado para profissional: " + prof.getEmail());
            } catch (Exception e) {
                System.err.println("⚠️ Falha ao enviar lembrete semanal (profissional): " + e.getMessage());
            }
        });

        // Para pacientes: status CONFIRMADA_PROFISSIONAL
        var pendentesPaciente = appointmentRepository.findPendentesNoFuturo(com.sistema.lucas.model.enums.StatusConsulta.CONFIRMADA_PROFISSIONAL);
        java.util.Map<com.sistema.lucas.model.Patient, Long> qtdePac = pendentesPaciente.stream()
            .collect(java.util.stream.Collectors.groupingBy(com.sistema.lucas.model.Appointment::getPatient, java.util.stream.Collectors.counting()));
        
        qtdePac.forEach((pac, qtde) -> {
            try {
                emailTemplateService.enviarLembreteSemanalPendenciasPaciente(pac, qtde);
                System.out.println("📧 Lembrete semanal enviado para paciente: " + pac.getEmail());
            } catch (Exception e) {
                System.err.println("⚠️ Falha ao enviar lembrete semanal (paciente): " + e.getMessage());
            }
        });
    }
}