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
                        <p style="margin:4px 0 0;color:rgba(255,255,255,0.8);font-size:13px;">Sistema Lucas — Gestão Clínica</p>
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
}