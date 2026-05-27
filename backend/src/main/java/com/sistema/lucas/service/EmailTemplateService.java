package com.sistema.lucas.service;

import com.sistema.lucas.model.Appointment;
import com.sistema.lucas.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
public class EmailTemplateService {

    @Autowired
    private EmailService emailService;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    // ─── Consulta agendada ───────────────────────────────────────────────────

    public void notificarConsultaAgendada(Appointment consulta) {
        String data = consulta.getDateTime().format(FMT);
        String nomePaciente = consulta.getPatient().getName();
        String nomeProfissional = consulta.getProfessional().getName();

        // Para o paciente
        emailService.enviar(
            consulta.getPatient().getEmail(),
            "Consulta agendada — " + data,
            buildTemplate(
                "Consulta agendada",
                "Olá, " + nomePaciente + "!",
                "Sua consulta foi agendada com sucesso.",
                new String[][]{
                    {"Profissional", nomeProfissional},
                    {"Data e hora", data},
                    {"Motivo", consulta.getReason() != null ? consulta.getReason() : "—"}
                },
                "Lembre-se de confirmar sua presença até 24h antes da consulta.",
                "#1e3a5f"
            )
        );

        // Para o profissional
        emailService.enviar(
            consulta.getProfessional().getEmail(),
            "Nova consulta agendada — " + data,
            buildTemplate(
                "Nova consulta agendada",
                "Olá, " + nomeProfissional + "!",
                "Uma nova consulta foi agendada na sua agenda.",
                new String[][]{
                    {"Paciente", nomePaciente},
                    {"Data e hora", data},
                    {"Motivo", consulta.getReason() != null ? consulta.getReason() : "—"}
                },
                null,
                "#1e3a5f"
            )
        );
    }

    // ─── Consulta confirmada por ambos ──────────────────────────────────────

    public void notificarConsultaConfirmada(Appointment consulta) {
        String data = consulta.getDateTime().format(FMT);
        String nomePaciente = consulta.getPatient().getName();
        String nomeProfissional = consulta.getProfessional().getName();

        // Para o paciente
        emailService.enviar(
            consulta.getPatient().getEmail(),
            "Consulta confirmada — " + data,
            buildTemplate(
                "Consulta confirmada",
                "Olá, " + nomePaciente + "!",
                "Sua consulta foi confirmada por ambas as partes.",
                new String[][]{
                    {"Profissional", nomeProfissional},
                    {"Data e hora", data}
                },
                "Até lá!",
                "#1a7a4a"
            )
        );

        // Para o profissional
        emailService.enviar(
            consulta.getProfessional().getEmail(),
            "Consulta confirmada — " + data,
            buildTemplate(
                "Consulta confirmada",
                "Olá, " + nomeProfissional + "!",
                "A consulta abaixo foi confirmada por ambas as partes.",
                new String[][]{
                    {"Paciente", nomePaciente},
                    {"Data e hora", data}
                },
                null,
                "#1a7a4a"
            )
        );
    }

    // ─── Consulta cancelada ──────────────────────────────────────────────────

    public void notificarConsultaCancelada(Appointment consulta) {
        String data = consulta.getDateTime().format(FMT);
        String nomePaciente = consulta.getPatient().getName();
        String nomeProfissional = consulta.getProfessional().getName();

        // Para o paciente
        emailService.enviar(
            consulta.getPatient().getEmail(),
            "Consulta cancelada — " + data,
            buildTemplate(
                "Consulta cancelada",
                "Olá, " + nomePaciente + "!",
                "Sua consulta foi cancelada.",
                new String[][]{
                    {"Profissional", nomeProfissional},
                    {"Data e hora", data}
                },
                "Se desejar, agende uma nova consulta pelo sistema.",
                "#a0320a"
            )
        );

        // Para o profissional
        emailService.enviar(
            consulta.getProfessional().getEmail(),
            "Consulta cancelada — " + data,
            buildTemplate(
                "Consulta cancelada",
                "Olá, " + nomeProfissional + "!",
                "A consulta abaixo foi cancelada.",
                new String[][]{
                    {"Paciente", nomePaciente},
                    {"Data e hora", data}
                },
                null,
                "#a0320a"
            )
        );
    }

    // ─── Lembrete 24h antes ──────────────────────────────────────────────────

    public void enviarLembrete(Appointment consulta) {
        String data = consulta.getDateTime().format(FMT);
        String nomePaciente = consulta.getPatient().getName();
        String nomeProfissional = consulta.getProfessional().getName();

        // Para o paciente
        emailService.enviar(
            consulta.getPatient().getEmail(),
            "Lembrete — consulta amanhã às " + consulta.getDateTime().format(DateTimeFormatter.ofPattern("HH:mm")),
            buildTemplate(
                "Lembrete de consulta",
                "Olá, " + nomePaciente + "!",
                "Você tem uma consulta agendada para amanhã.",
                new String[][]{
                    {"Profissional", nomeProfissional},
                    {"Data e hora", data}
                },
                "Caso não possa comparecer, cancele pelo sistema com antecedência.",
                "#5a3d9e"
            )
        );

        // Para o profissional
        emailService.enviar(
            consulta.getProfessional().getEmail(),
            "Lembrete — consulta amanhã às " + consulta.getDateTime().format(DateTimeFormatter.ofPattern("HH:mm")),
            buildTemplate(
                "Lembrete de consulta",
                "Olá, " + nomeProfissional + "!",
                "Você tem uma consulta agendada para amanhã.",
                new String[][]{
                    {"Paciente", nomePaciente},
                    {"Data e hora", data}
                },
                null,
                "#5a3d9e"
            )
        );
    }
    
    // ─── Fluxo de Aprovação ──────────────────────────────────────────────────

    public void notificarSolicitacaoAgendamentoParaMedico(Appointment consulta) {
        String data = consulta.getDateTime().format(FMT);
        emailService.enviar(
            consulta.getProfessional().getEmail(),
            "Nova solicitação de consulta — " + data,
            buildTemplate(
                "Solicitação de Agendamento Pendente",
                "Olá, Dr(a). " + consulta.getProfessional().getName() + "!",
                "Um paciente solicitou um horário na sua agenda e aguarda sua confirmação.",
                new String[][]{
                    {"Paciente", consulta.getPatient().getName()},
                    {"Data e hora", data},
                    {"Motivo", consulta.getReason() != null ? consulta.getReason() : "—"}
                },
                "Acesse seu painel profissional para Confirmar ou Recusar esta consulta.",
                "#5a3d9e"
            )
        );
    }

    public void notificarPacienteAgendamentoPendente(Appointment consulta) {
        String data = consulta.getDateTime().format(FMT);
        emailService.enviar(
            consulta.getPatient().getEmail(),
            "Solicitação de consulta enviada — " + data,
            buildTemplate(
                "Solicitação Recebida",
                "Olá, " + consulta.getPatient().getName() + "!",
                "Sua solicitação de consulta foi enviada com sucesso e está aguardando a aprovação do médico.",
                new String[][]{
                    {"Profissional", consulta.getProfessional().getName()},
                    {"Data e hora", data}
                },
                "Você receberá uma confirmação por e-mail assim que o médico validar o agendamento.",
                "#1e3a5f"
            )
        );
    }

    public void notificarPacienteAgendamentoAceito(Appointment consulta) {
        String data = consulta.getDateTime().format(FMT);
        emailService.enviar(
            consulta.getPatient().getEmail(),
            "Consulta Confirmada! — " + data,
            buildTemplate(
                "Sua Consulta foi Aprovada",
                "Olá, " + consulta.getPatient().getName() + "!",
                "Excelente! O médico confirmou a sua solicitação de agendamento.",
                new String[][]{
                    {"Profissional", consulta.getProfessional().getName()},
                    {"Data e hora", data}
                },
                "Lembre-se de comparecer ou cancelar com no mínimo 24h de antecedência se houver imprevistos.",
                "#1a7a4a"
            )
        );
    }

    public void notificarPacienteAgendamentoRecusado(Appointment consulta, String motivo) {
        String data = consulta.getDateTime().format(FMT);
        emailService.enviar(
            consulta.getPatient().getEmail(),
            "Solicitação de consulta recusada — " + data,
            buildTemplate(
                "Solicitação Não Aprovada",
                "Olá, " + consulta.getPatient().getName() + "!",
                "Infelizmente o médico não pôde aceitar a sua solicitação de consulta.",
                new String[][]{
                    {"Profissional", consulta.getProfessional().getName()},
                    {"Data e hora", data},
                    {"Motivo da recusa", motivo != null ? motivo : "Indisponibilidade de agenda"}
                },
                "Você pode tentar agendar em outro horário disponível no sistema.",
                "#a0320a"
            )
        );
    }

    // ─── Penalidades por Falta / Cancelamento Tardio ────────────────────────

    public void enviarAvisoPrimeiraFalta(Patient paciente, Appointment consulta) {
        String dataStr = consulta.getDateTime().format(FMT);
        emailService.enviar(
            paciente.getEmail(),
            "Aviso importante: Política de Faltas — Projeto Lucas",
            buildTemplate(
                "Aviso de Ausência",
                "Olá, " + paciente.getName() + "!",
                "Registramos que você não pôde comparecer à consulta agendada para <b>" + dataStr + "</b> e não realizou o cancelamento com antecedência mínima de 24 horas.",
                new String[][]{
                    {"Profissional", consulta.getProfessional().getName()},
                    {"Consulta em questão", dataStr}
                },
                "⚠️ <b>Atenção:</b> Esta é uma advertência formal. Caso ocorra uma segunda falta sem cancelamento prévio, sua conta será bloqueada temporariamente para novos agendamentos por 15 dias.",
                "#f59e0b"
            )
        );
    }

    public void enviarAvisoBloqueioFalta(Patient paciente, Appointment consulta, java.time.LocalDateTime bloqueadoAte) {
        String dataFalta = consulta.getDateTime().format(FMT);
        String dataLiberacao = bloqueadoAte.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        emailService.enviar(
            paciente.getEmail(),
            "Acesso Temporariamente Suspenso — Projeto Lucas",
            buildTemplate(
                "Bloqueio de Agendamentos",
                "Olá, " + paciente.getName() + "!",
                "Identificamos uma reincidência de faltas em agendamentos (ausência em <b>" + dataFalta + "</b> sem aviso prévio de 24h).",
                new String[][]{
                    {"Infração ocorrida em", dataFalta},
                    {"Agendamentos suspensos até", dataLiberacao}
                },
                "Conforme nossa política clínica, o agendamento de novas consultas está temporariamente suspenso para a sua conta até a data informada.",
                "#b91c1c"
            )
        );
    }

    // ─── Lembrete de Prazo de Agenda ──────────────────────────────────────────
    
    public void enviarLembreteSubmissaoAgenda(com.sistema.lucas.model.Professional prof, java.time.YearMonth mesAlvo, boolean urgente) {
        String nomeMes = mesAlvo.getMonth().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.forLanguageTag("pt-BR"));
        nomeMes = nomeMes.substring(0, 1).toUpperCase() + nomeMes.substring(1);
        
        String urlSistema = frontendUrl;
        
        String prefixo = urgente ? "🚨 URGENTE: " : "⚠️ Lembrete: ";
        String mensagem = urgente 
            ? "O mês está acabando! Notamos que sua disponibilidade para <b>" + nomeMes + "</b> ainda não foi preenchida."
            : "Gostaríamos de lembrar que o prazo para submissão da sua disponibilidade para o próximo mês (<b>" + nomeMes + "</b>) encerra em breve.";
        
        emailService.enviar(
            prof.getEmail(),
            prefixo + "Prazo final para agenda de " + nomeMes,
            buildPremiumTemplate(
                urgente ? "Ação Necessária: Agenda" : "Lembrete de Agenda",
                "Olá, Dr(a). " + prof.getName() + "!",
                mensagem,
                urgente ? "⏳ Prazo Crítico" : "Prazo Final: Dia 25 às 23:59",
                urgente ? "Sua agenda precisa ser preenchida hoje para evitar transtornos no agendamento de pacientes." 
                        : "Após este período, iniciaremos os avisos diários de pendência.",
                "Atualizar Minha Agenda Agora",
                urlSistema + "/panel/availability"
            )
        );
    }

    // ─── Lembretes Semanais de Pendências ───────────────────────────────────

    public void enviarLembreteSemanalPendenciasProfissional(com.sistema.lucas.model.Professional prof, long qtdePendentes) {
        String urlSistema = frontendUrl + "/panel/professional-appointments";
        emailService.enviar(
            prof.getEmail(),
            "Lembrete: Você tem agendamentos pendentes de confirmação",
            buildPremiumTemplate(
                "Agendamentos Pendentes",
                "Olá, Dr(a). " + prof.getName() + "!",
                "Você tem <b>" + qtdePendentes + "</b> agendamento(s) pendente(s) de aprovação na sua agenda.",
                "Ação Necessária",
                "Por favor, acesse o sistema para Confirmar ou Recusar as consultas solicitadas pelos pacientes.",
                "Acessar Painel do Profissional",
                urlSistema
            )
        );
    }

    public void enviarLembreteSemanalPendenciasPaciente(Patient paciente, long qtdePendentes) {
        String urlSistema = frontendUrl + "/panel/my-appointments";
        emailService.enviar(
            paciente.getEmail(),
            "Lembrete: Confirme sua presença na consulta",
            buildPremiumTemplate(
                "Confirmação de Presença",
                "Olá, " + paciente.getName() + "!",
                "Você tem <b>" + qtdePendentes + "</b> consulta(s) aguardando sua confirmação de presença.",
                "Confirmação Necessária",
                "Por favor, acesse o sistema para confirmar sua presença ou cancelar o agendamento com antecedência.",
                "Confirmar Minha Presença",
                urlSistema
            )
        );
    }

    // ─── Alerta de Consultas Atrasadas ───────────────────────────────────────

    public void enviarAlertaConsultasAtrasadas(com.sistema.lucas.model.Professional profissional,
                                               java.util.List<Appointment> atrasadas) {
        String urlAgenda = frontendUrl + "/panel/professional-appointments";
        int total = atrasadas.size();

        StringBuilder lista = new StringBuilder();
        for (Appointment a : atrasadas) {
            lista.append("• ")
                 .append(a.getDateTime().format(FMT))
                 .append(" — ")
                 .append(a.getPatient().getName())
                 .append(" (").append(a.getStatus().name()).append(")")
                 .append("<br>");
        }

        emailService.enviar(
            profissional.getEmail(),
            "⚠️ URGENTE: " + total + " consulta(s) com status pendente após a data",
            buildPremiumTemplate(
                "Consultas Atrasadas — Ação Urgente",
                "Olá, Dr(a). " + profissional.getName() + "!",
                "As consultas abaixo já passaram da data/hora agendada, mas ainda estão com status pendente.<br><br>" + lista,
                "Ação imediata necessária",
                "Acesse o sistema e atualize o status de cada consulta (Concluída, Paciente Faltou ou Cancelada).",
                "Ver Consultas Atrasadas",
                urlAgenda
            )
        );
    }

    // ─── Template HTML base ──────────────────────────────────────────────────

    private String buildTemplate(
            String titulo,
            String saudacao,
            String mensagem,
            String[][] campos,
            String rodape,
            String corDestaque) {

        StringBuilder linhas = new StringBuilder();
        for (String[] campo : campos) {
            linhas.append("""
                <tr>
                  <td style="padding:16px;font-size:14px;color:#64748b;width:140px;vertical-align:top;border-bottom:1px solid #e2e8f0;">%s</td>
                  <td style="padding:16px;font-size:14px;color:#0f172a;font-weight:600;border-bottom:1px solid #e2e8f0;">%s</td>
                </tr>
                """.formatted(campo[0], campo[1]));
        }

        String rodapeHtml = rodape != null
            ? "<div style='background-color:#fff7ed;border-left:4px solid #f97316;padding:20px;border-radius:8px;margin-bottom:10px;'><p style='margin:0;font-size:14px;color:#9a3412;'>%s</p></div>".formatted(rodape)
            : "";

        return """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background-color:#f8fafc;font-family:Arial,sans-serif;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f8fafc;padding:40px 0;">
                    <tr><td align="center">
                        <table width="600" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 10px 25px rgba(0,0,0,0.05);border:1px solid #e2e8f0;">
                            <tr>
                                <td style="background: %s; padding:40px;">
                                    <h1 style="margin:0;color:#ffffff;font-size:24px;font-weight:700;">%s</h1>
                                    <p style="margin:8px 0 0;color:rgba(255,255,255,0.85);font-size:14px;">Projeto Lucas — Gestão Clínica</p>
                                </td>
                            </tr>
                            <tr>
                                <td style="padding:40px;">
                                    <p style="margin:0 0 20px;font-size:18px;color:#1e293b;font-weight:600;">%s</p>
                                    <p style="margin:0 0 24px;font-size:16px;line-height:1.6;color:#475569;">%s</p>

                                    <!-- Tabela de detalhes -->
                                    <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f8fafc;border-radius:10px;border:1px solid #e2e8f0;margin-bottom:24px;">
                                        %s
                                    </table>

                                    %s
                                    
                                    <table width="100%%" cellpadding="0" cellspacing="0" style="margin-top:30px;">
                                        <tr><td align="center">
                                            <a href="%s" style="background-color:#2563eb;color:#ffffff;padding:16px 32px;text-decoration:none;font-weight:700;border-radius:10px;font-size:16px;display:inline-block;">
                                                Acessar o Sistema
                                            </a>
                                        </td></tr>
                                    </table>
                                </td>
                            </tr>
                            <tr>
                                <td style="padding:30px;background-color:#f1f5f9;text-align:center;">
                                    <p style="margin:0;font-size:12px;color:#94a3b8;">Projeto Lucas — Gestão Clínica Humanizada</p>
                                    <p style="margin:5px 0 0;font-size:11px;color:#cbd5e1;">Este é um e-mail automático. Por favor, não responda.</p>
                                </td>
                            </tr>
                        </table>
                    </td></tr>
                </table>
            </body>
            </html>
            """.formatted(corDestaque, titulo, saudacao, mensagem, linhas, rodapeHtml, frontendUrl);
    }

    private String buildPremiumTemplate(
            String titulo,
            String saudacao,
            String mensagem,
            String destaqueTitulo,
            String destaqueTexto,
            String textoCta,
            String urlCta) {
        
        return """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background-color:#f8fafc;font-family:Arial,sans-serif;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f8fafc;padding:40px 0;">
                    <tr><td align="center">
                        <table width="600" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 10px 25px rgba(0,0,0,0.05);border:1px solid #e2e8f0;">
                            <tr>
                                <td style="background: linear-gradient(135deg, #1e3a8a 0%%, #3b82f6 100%%); padding:40px;">
                                    <h1 style="margin:0;color:#ffffff;font-size:24px;font-weight:700;">%s</h1>
                                    <p style="margin:8px 0 0;color:rgba(255,255,255,0.85);font-size:14px;">Projeto Lucas — Gestão Clínica</p>
                                </td>
                            </tr>
                            <tr>
                                <td style="padding:40px;">
                                    <p style="margin:0 0 20px;font-size:18px;color:#1e293b;font-weight:600;">%s</p>
                                    <p style="margin:0 0 24px;font-size:16px;line-height:1.6;color:#475569;">%s</p>
                                    <div style="background-color:#fff7ed;border-left:4px solid #f97316;padding:20px;border-radius:8px;margin-bottom:30px;">
                                        <p style="margin:0;font-size:15px;color:#9a3412;font-weight:600;">%s</p>
                                        <p style="margin:5px 0 0;font-size:14px;color:#c2410c;">%s</p>
                                    </div>
                                    <table width="100%%" cellpadding="0" cellspacing="0">
                                        <tr><td align="center">
                                            <a href="%s" style="background-color:#2563eb;color:#ffffff;padding:16px 32px;text-decoration:none;font-weight:700;border-radius:10px;font-size:16px;display:inline-block;">
                                                %s
                                            </a>
                                        </td></tr>
                                    </table>
                                </td>
                            </tr>
                            <tr>
                                <td style="padding:30px;background-color:#f1f5f9;text-align:center;">
                                    <p style="margin:0;font-size:12px;color:#94a3b8;">Projeto Lucas — Gestão Clínica Humanizada</p>
                                </td>
                            </tr>
                        </table>
                    </td></tr>
                </table>
            </body>
            </html>
            """.formatted(titulo, saudacao, mensagem, destaqueTitulo, destaqueTexto, urlCta, textoCta);
    }
}