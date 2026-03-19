// backend/src/main/java/com/sistema/lucas/config/DataInitializer.java
package com.sistema.lucas.config;

import com.sistema.lucas.model.*;
import com.sistema.lucas.model.enums.Role;
import com.sistema.lucas.model.enums.StatusConsulta;
import com.sistema.lucas.model.enums.TipoDocumento;
import com.sistema.lucas.model.enums.TipoRegistro;
import com.sistema.lucas.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
public class DataInitializer implements CommandLineRunner {

    @Autowired private UserRepository userRepository;
    @Autowired private ProfessionalRepository professionalRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private ProntuarioRepository prontuarioRepository;
    @Autowired private DocumentoRepository documentoRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() > 0) {
            System.out.println("ℹ️ Banco já populado. Pulando carga de demonstração.");
            return;
        }

        try {
            System.out.println("🌱 Carregando dados de demonstração...");
            LocalDateTime agora = LocalDateTime.now();

            // ─── ADMIN ───────────────────────────────────────────────────────
            User admin = new User();
            admin.setName("Administrador");
            admin.setEmail("admin@clinica.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);

            // ─── PROFISSIONAIS ───────────────────────────────────────────────

            // Psicóloga principal
            Professional ana = new Professional();
            ana.setName("Dra. Ana Souza");
            ana.setEmail("ana@clinica.com");
            ana.setPassword(passwordEncoder.encode("123456"));
            ana.setRole(Role.PROFESSIONAL);
            ana.setTipoRegistro(TipoRegistro.CRP);
            ana.setRegistroConselho("CRP-06 123456");
            ana.setSpecialty("Psicologia Clínica");
            professionalRepository.save(ana);

            // Segundo profissional — médico psiquiatra
            Professional carlos = new Professional();
            carlos.setName("Dr. Carlos Menezes");
            carlos.setEmail("carlos@clinica.com");
            carlos.setPassword(passwordEncoder.encode("123456"));
            carlos.setRole(Role.PROFESSIONAL);
            carlos.setTipoRegistro(TipoRegistro.CRM);
            carlos.setRegistroConselho("CRM-SP 654321");
            carlos.setSpecialty("Psiquiatria");
            professionalRepository.save(carlos);

            // ─── PACIENTES ───────────────────────────────────────────────────

            Patient lucas = new Patient();
            lucas.setName("Lucas Silva");
            lucas.setEmail("lucas@email.com");
            lucas.setPassword(passwordEncoder.encode("123456"));
            lucas.setRole(Role.PATIENT);
            lucas.setCpf("111.222.333-44");
            lucas.setPhone("11888889999");
            patientRepository.save(lucas);

            Patient maria = new Patient();
            maria.setName("Maria Oliveira");
            maria.setEmail("maria@email.com");
            maria.setPassword(passwordEncoder.encode("123456"));
            maria.setRole(Role.PATIENT);
            maria.setCpf("222.333.444-55");
            maria.setPhone("11999998888");
            patientRepository.save(maria);

            Patient joao = new Patient();
            joao.setName("João Pereira");
            joao.setEmail("joao@email.com");
            joao.setPassword(passwordEncoder.encode("123456"));
            joao.setRole(Role.PATIENT);
            joao.setCpf("333.444.555-66");
            joao.setPhone("11999990003");
            patientRepository.save(joao);

            // ─── CONSULTAS ───────────────────────────────────────────────────

            // Passadas — concluídas (para testar histórico e dashboard)
            Appointment c1 = new Appointment(ana, lucas, agora.minusDays(14), "Avaliação inicial", StatusConsulta.CONCLUIDA);
            Appointment c2 = new Appointment(ana, lucas, agora.minusDays(7), "Sessão de acompanhamento", StatusConsulta.CONCLUIDA);
            Appointment c3 = new Appointment(ana, maria, agora.minusDays(10), "Consulta inicial", StatusConsulta.CONCLUIDA);
            Appointment c4 = new Appointment(carlos, lucas, agora.minusDays(5), "Avaliação psiquiátrica", StatusConsulta.CONCLUIDA);

            // Passada — faltou (para testar status FALTA)
            Appointment c5 = new Appointment(ana, joao, agora.minusDays(3), "Primeira consulta", StatusConsulta.FALTA);

            // Hoje — para testar a agenda do profissional
            Appointment c6 = new Appointment(ana, lucas, agora.withHour(9).withMinute(0).withSecond(0), "Sessão semanal", StatusConsulta.CONFIRMADA);
            Appointment c7 = new Appointment(ana, maria, agora.withHour(11).withMinute(0).withSecond(0), "Retorno", StatusConsulta.CONFIRMADA_PACIENTE);
            Appointment c8 = new Appointment(carlos, joao, agora.withHour(14).withMinute(30).withSecond(0), "Consulta de rotina", StatusConsulta.AGENDADA);

            // Futuras — para testar confirmação e lembrete
            Appointment c9  = new Appointment(ana, lucas, agora.plusDays(7).withHour(10).withMinute(0).withSecond(0), "Sessão de acompanhamento", StatusConsulta.AGENDADA);
            Appointment c10 = new Appointment(ana, maria, agora.plusDays(3).withHour(15).withMinute(0).withSecond(0), "Retorno quinzenal", StatusConsulta.AGENDADA);
            Appointment c11 = new Appointment(carlos, lucas, agora.plusDays(14).withHour(9).withMinute(0).withSecond(0), "Revisão de medicação", StatusConsulta.CONFIRMADA_PACIENTE);

            // Cancelada — para testar status CANCELADA
            Appointment c12 = new Appointment(ana, joao, agora.plusDays(2).withHour(16).withMinute(0).withSecond(0), "Consulta inicial", StatusConsulta.CANCELADA);

            appointmentRepository.save(c1);
            appointmentRepository.save(c2);
            appointmentRepository.save(c3);
            appointmentRepository.save(c4);
            appointmentRepository.save(c5);
            appointmentRepository.save(c6);
            appointmentRepository.save(c7);
            appointmentRepository.save(c8);
            appointmentRepository.save(c9);
            appointmentRepository.save(c10);
            appointmentRepository.save(c11);
            appointmentRepository.save(c12);

            // ─── PRONTUÁRIOS ─────────────────────────────────────────────────

            Prontuario p1 = new Prontuario();
            p1.setAppointment(c1);
            p1.setPatient(lucas);
            p1.setProfessional(ana);
            p1.setNotas("Primeira sessão. Paciente relata ansiedade generalizada há aproximadamente 8 meses, com piora nos últimos 2 meses. Queixas de insônia e dificuldade de concentração no trabalho. Acordado início de acompanhamento semanal.");
            prontuarioRepository.save(p1);

            Prontuario p2 = new Prontuario();
            p2.setAppointment(c2);
            p2.setPatient(lucas);
            p2.setProfessional(ana);
            p2.setNotas("Segunda sessão. Paciente relata leve melhora na qualidade do sono após técnicas de respiração orientadas. Ainda apresenta episódios de ansiedade no ambiente de trabalho. Iniciado trabalho de reestruturação cognitiva.");
            prontuarioRepository.save(p2);

            Prontuario p3 = new Prontuario();
            p3.setAppointment(c3);
            p3.setPatient(maria);
            p3.setProfessional(ana);
            p3.setNotas("Consulta inicial. Paciente procura atendimento após término de relacionamento de longa duração. Apresenta tristeza persistente e isolamento social. Descartado episódio depressivo maior. Proposto acompanhamento quinzenal.");
            prontuarioRepository.save(p3);

            Prontuario p4 = new Prontuario();
            p4.setAppointment(c4);
            p4.setPatient(lucas);
            p4.setProfessional(carlos);
            p4.setNotas("Avaliação psiquiátrica solicitada pela Dra. Ana. Paciente apresenta sintomas compatíveis com TAG. Iniciado tratamento farmacológico com dose baixa. Retorno em 30 dias para avaliação de resposta.");
            prontuarioRepository.save(p4);

            // ─── DOCUMENTOS ──────────────────────────────────────────────────

            // Laudo disponibilizado para Lucas
            Documento d1 = new Documento();
            d1.setTipo(TipoDocumento.LAUDO_PSICOLOGICO);
            d1.setTitulo("Laudo Psicológico — Lucas Silva");
            d1.setConteudoTexto(
                "LAUDO PSICOLÓGICO\n\n" +
                "Paciente: Lucas Silva\n" +
                "Data de avaliação: " + agora.minusDays(7).toLocalDate() + "\n" +
                "Profissional responsável: Dra. Ana Souza — CRP-06 123456\n\n" +
                "SÍNTESE CLÍNICA:\n" +
                "O paciente apresenta quadro compatível com Transtorno de Ansiedade Generalizada (F41.1 — CID-10), " +
                "com início há aproximadamente 8 meses. Manifesta preocupação excessiva, tensão muscular, " +
                "insônia e dificuldade de concentração.\n\n" +
                "CONCLUSÃO:\n" +
                "Recomenda-se continuidade do acompanhamento psicológico semanal e avaliação psiquiátrica " +
                "para suporte farmacológico complementar."
            );
            d1.setPaciente(lucas);
            d1.setProfissional(ana);
            d1.setDisponivel(true);
            documentoRepository.save(d1);

            // Atestado para Maria
            Documento d2 = new Documento();
            d2.setTipo(TipoDocumento.ATESTADO);
            d2.setTitulo("Atestado de Comparecimento — Maria Oliveira");
            d2.setConteudoTexto(
                "ATESTADO DE COMPARECIMENTO\n\n" +
                "Atesto que Maria Oliveira, CPF 222.333.444-55, " +
                "esteve presente nesta clínica em sessão de psicoterapia em " +
                agora.minusDays(10).toLocalDate() + ", das 14h00 às 15h00.\n\n" +
                "Dra. Ana Souza\nCRP-06 123456\nPsicóloga Clínica"
            );
            d2.setPaciente(maria);
            d2.setProfissional(ana);
            d2.setDisponivel(true);
            documentoRepository.save(d2);

            // Encaminhamento para Lucas — ainda não disponibilizado (rascunho)
            Documento d3 = new Documento();
            d3.setTipo(TipoDocumento.ENCAMINHAMENTO);
            d3.setTitulo("Encaminhamento para Psiquiatria — Lucas Silva");
            d3.setConteudoTexto(
                "ENCAMINHAMENTO\n\n" +
                "Encaminho o paciente Lucas Silva para avaliação psiquiátrica, " +
                "considerando quadro de ansiedade generalizada com indicação de suporte farmacológico.\n\n" +
                "Dra. Ana Souza — CRP-06 123456"
            );
            d3.setPaciente(lucas);
            d3.setProfissional(ana);
            d3.setDisponivel(false); // rascunho — testa o toggle de disponibilidade
            documentoRepository.save(d3);

            System.out.println("✅ Dados de demonstração carregados com sucesso!");
            System.out.println("─────────────────────────────────────────────");
            System.out.println("👤 ADMIN       → admin@clinica.com     / admin123");
            System.out.println("🩺 PROFISSIONAL → ana@clinica.com       / 123456");
            System.out.println("🩺 PROFISSIONAL → carlos@clinica.com    / 123456");
            System.out.println("🙋 PACIENTE    → lucas@email.com       / 123456");
            System.out.println("🙋 PACIENTE    → maria@email.com       / 123456");
            System.out.println("🙋 PACIENTE    → joao@email.com        / 123456");
            System.out.println("─────────────────────────────────────────────");

        } catch (Exception e) {
            System.err.println("⚠️ Erro na carga: " + e.getMessage());
            e.printStackTrace();
        }
    }
}