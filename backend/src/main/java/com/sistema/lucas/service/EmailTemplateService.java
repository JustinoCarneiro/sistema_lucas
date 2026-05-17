package com.sistema.lucas.service;

import com.sistema.lucas.model.Appointment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
public class EmailTemplateService {

    @Autowired
    private EmailService emailService;

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
    
    // ─── Lembrete de Prazo de Agenda ──────────────────────────────────────────
    
    public void enviarLembreteSubmissaoAgenda(com.sistema.lucas.model.Professional prof, java.time.YearMonth mesAlvo, boolean urgente) {
        String nomeMes = mesAlvo.getMonth().getDisplayName(java.time.format.TextStyle.FULL, new java.util.Locale("pt", "BR"));
        nomeMes = nomeMes.substring(0, 1).toUpperCase() + nomeMes.substring(1);
        
        String urlSistema = "http://localhost:8082"; // Idealmente viria de um config
        
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
                urlSistema + "/panel/availability"
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
                  <td style="padding:8px 12px;font-size:14px;color:#555;width:140px;vertical-align:top;">%s</td>
                  <td style="padding:8px 12px;font-size:14px;color:#111;font-weight:600;">%s</td>
                </tr>
                """.formatted(campo[0], campo[1]));
        }

        String rodapeHtml = rodape != null
            ? "<p style='margin:20px 0 0;font-size:13px;color:#777;'>%s</p>".formatted(rodape)
            : "";

        return """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background:#f4f4f4;font-family:Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f4f4;padding:40px 0;">
                <tr><td align="center">
                  <table width="560" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.08);">

                    <!-- Cabeçalho -->
                    <tr>
                      <td style="background:%s;padding:28px 32px;">
                        <h1 style="margin:0;color:#ffffff;font-size:20px;font-weight:700;">%s</h1>
                        <p style="margin:4px 0 0;color:rgba(255,255,255,0.8);font-size:13px;">Projeto Lucas — Gestão Clínica</p>
                      </td>
                    </tr>

                    <!-- Corpo -->
                    <tr>
                      <td style="padding:28px 32px;">
                        <p style="margin:0 0 16px;font-size:16px;color:#222;">%s</p>
                        <p style="margin:0 0 20px;font-size:14px;color:#555;">%s</p>

                        <!-- Tabela de detalhes -->
                        <table width="100%%" cellpadding="0" cellspacing="0"
                          style="background:#f9f9f9;border-radius:8px;border:1px solid #ececec;">
                          %s
                        </table>

                        %s
                      </td>
                    </tr>

                    <!-- Rodapé -->
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
            """.formatted(corDestaque, titulo, saudacao, mensagem, linhas, rodapeHtml);
    }

    private String buildPremiumTemplate(
            String titulo,
            String saudacao,
            String mensagem,
            String destaqueTitulo,
            String destaqueTexto,
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
                                                Atualizar Minha Agenda Agora
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
            """.formatted(titulo, saudacao, mensagem, destaqueTitulo, destaqueTexto, urlCta);
    }
}