// backend/src/main/java/com/sistema/lucas/service/WaitlistService.java
package com.sistema.lucas.service;

import com.sistema.lucas.event.ConsultaCanceladaEvent;
import com.sistema.lucas.model.Appointment;
import com.sistema.lucas.model.Professional;
import com.sistema.lucas.model.WaitlistEntry;
import com.sistema.lucas.model.dto.WaitlistEntryResponseDTO;
import com.sistema.lucas.model.dto.WaitlistOfertaStatusDTO;
import com.sistema.lucas.model.enums.StatusConsulta;
import com.sistema.lucas.model.enums.WaitlistStatus;
import com.sistema.lucas.repository.AppointmentRepository;
import com.sistema.lucas.repository.PatientRepository;
import com.sistema.lucas.repository.ProfessionalAvailabilityRepository;
import com.sistema.lucas.repository.ProfessionalRepository;
import com.sistema.lucas.repository.WaitlistEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// M13 (E4/US-4.8) — versão robusta: a vaga é reservada ativamente pro primeiro da fila
// (cria a Appointment na hora, o que já ocupa o horário pra qualquer outro paciente) e só
// vira definitiva se ele confirmar dentro do prazo. Sem confirmação a tempo, o
// WaitlistExpirationScheduler cancela essa reserva e oferece pro próximo da fila.
@Service
public class WaitlistService {

    private static final Logger logger = LoggerFactory.getLogger(WaitlistService.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    @Autowired private WaitlistEntryRepository waitlistEntryRepository;
    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private ProfessionalRepository professionalRepository;
    @Autowired private ProfessionalAvailabilityRepository availabilityRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private AppointmentService appointmentService;
    @Autowired private EmailTemplateService emailTemplateService;
    @Autowired private AuditLogService auditLogService;

    // Auto-referência (@Lazy evita ciclo na criação do bean): usada só pra rotear chamadas que,
    // de outra forma, seriam self-invocation (this.metodo(...) dentro da própria classe) —
    // self-invocation não passa pelo proxy AOP do Spring, então @Transactional vira letra morta
    // nesses casos. Ver ofertarProximoDaFila().
    @Autowired @Lazy private WaitlistService self;

    @Value("${app.waitlist.oferta.horas:2}")
    private long horasOferta;

    // ─── Entrar / sair da fila ─────────────────────────────────────────────────

    @Transactional
    public void entrarNaFila(Long professionalId, LocalDateTime dateTime, String emailPaciente) {
        var professional = professionalRepository.findById(professionalId)
            .orElseThrow(() -> new RuntimeException("Profissional não encontrado"));
        var paciente = patientRepository.findByEmail(emailPaciente)
            .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));

        if (paciente.getBlockedUntil() != null && paciente.getBlockedUntil().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Você está temporariamente bloqueado para novos agendamentos e não pode entrar na lista de espera.");
        }

        if (!appointmentService.horarioEstaOcupado(professionalId, dateTime)) {
            throw new RuntimeException("Esse horário está livre — você já pode agendar normalmente.");
        }

        boolean jaNaFila = waitlistEntryRepository.existsByPatientIdAndProfessionalIdAndDateTimeAndStatus(
            paciente.getId(), professionalId, dateTime, WaitlistStatus.AGUARDANDO);
        if (jaNaFila) {
            throw new RuntimeException("Você já está na lista de espera para esse horário.");
        }

        var entry = new WaitlistEntry();
        entry.setProfessional(professional);
        entry.setPatient(paciente);
        entry.setDateTime(dateTime);
        waitlistEntryRepository.save(entry);

        auditLogService.log(emailPaciente, "ENTRADA_LISTA_ESPERA", "WaitlistEntry", entry.getId(),
            "Entrou na lista de espera pro profissional ID " + professionalId + " em " + dateTime);
    }

    @Transactional(readOnly = true)
    public List<WaitlistEntryResponseDTO> minhasEntradas(String emailPaciente) {
        return waitlistEntryRepository.findByPatientEmailOrderByCriadoEmDesc(emailPaciente)
            .stream().map(WaitlistEntryResponseDTO::new).toList();
    }

    @Transactional
    public void sairDaFila(Long entryId, String emailPaciente) {
        var entry = waitlistEntryRepository.findById(entryId)
            .orElseThrow(() -> new RuntimeException("Entrada não encontrada."));

        if (!entry.getPatient().getEmail().equals(emailPaciente)) {
            throw new RuntimeException("Você não pode remover a entrada de outra pessoa.");
        }
        if (entry.getStatus() != WaitlistStatus.AGUARDANDO) {
            throw new RuntimeException("Só é possível sair da fila enquanto ainda está aguardando.");
        }

        entry.setStatus(WaitlistStatus.CANCELADA);
        waitlistEntryRepository.save(entry);

        auditLogService.log(emailPaciente, "SAIDA_LISTA_ESPERA", "WaitlistEntry", entryId, "Paciente saiu da lista de espera.");
    }

    // ─── Reagir a mudanças na consulta (evento) ─────────────────────────────────

    // AFTER_COMMIT: só oferece a vaga depois que o cancelamento/liberação realmente foi
    // persistido. Escutar um evento (em vez de AppointmentService injetar WaitlistService
    // direto) evita a dependência circular AppointmentService <-> WaitlistService (Spring Boot
    // recusa resolver por padrão). Falha aqui é efeito colateral não-crítico — nunca deve
    // propagar. IMPORTANTE: chama self.ofertarProximoDaFila (não this.) — este método listener
    // não abre transação própria (AFTER_COMMIT já roda fora de qualquer transação ativa), e uma
    // chamada this.ofertarProximoDaFila aqui seria self-invocation: bypassa o proxy AOP do
    // Spring e o @Transactional de ofertarProximoDaFila não teria efeito nenhum.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoCancelarConsulta(ConsultaCanceladaEvent event) {
        try {
            sincronizarEntradaCancelada(event.appointmentId());
            self.ofertarProximoDaFila(event.professionalId(), event.dateTime());
        } catch (Exception e) {
            logger.warn("Falha ao ofertar vaga da lista de espera após cancelamento: {}", e.getMessage());
        }
    }

    // Se a consulta liberada tinha uma WaitlistEntry vinculada (era ela mesma fruto de uma
    // oferta anterior), sincroniza a entrada como cancelada — evita que ela fique órfã, presa
    // num status (OFERECIDA/CONFIRMADA) que não reflete mais a realidade.
    private void sincronizarEntradaCancelada(Long appointmentId) {
        waitlistEntryRepository.findByAppointmentId(appointmentId).ifPresent(entry -> {
            if (entry.getStatus() == WaitlistStatus.OFERECIDA || entry.getStatus() == WaitlistStatus.CONFIRMADA) {
                entry.setStatus(WaitlistStatus.CANCELADA);
                waitlistEntryRepository.save(entry);
            }
        });
    }

    // ─── Oferecer a vaga ao próximo da fila ─────────────────────────────────────

    // Chamado (via self., nunca this. — ver nota acima) pelo listener de cancelamento e por
    // expirarOfertasVencidas() (vaga não confirmada a tempo — cascata pro próximo da fila).
    @Transactional
    public void ofertarProximoDaFila(Long professionalId, LocalDateTime dateTime) {
        Optional<WaitlistEntry> proximo = waitlistEntryRepository
            .findFirstByProfessionalIdAndDateTimeAndStatusOrderByCriadoEmAsc(professionalId, dateTime, WaitlistStatus.AGUARDANDO);

        while (proximo.isPresent()) {
            var entry = proximo.get();
            var paciente = entry.getPatient();

            // Paciente bloqueado por penalidade não pode receber a oferta — pula pro próximo.
            if (paciente.getBlockedUntil() != null && paciente.getBlockedUntil().isAfter(LocalDateTime.now())) {
                entry.setStatus(WaitlistStatus.CANCELADA);
                waitlistEntryRepository.save(entry);
                proximo = waitlistEntryRepository
                    .findFirstByProfessionalIdAndDateTimeAndStatusOrderByCriadoEmAsc(professionalId, dateTime, WaitlistStatus.AGUARDANDO);
                continue;
            }

            // Revalida o conflito: entre o horário liberar e este método rodar (AFTER_COMMIT já
            // é uma janela fora da transação original), um agendamento normal concorrente pode
            // ter ocupado o mesmo horário. Se ocupou, não há vaga real pra oferecer.
            if (appointmentService.horarioEstaOcupado(professionalId, dateTime)) {
                return;
            }

            // Revalida a disponibilidade: o profissional pode ter editado a grade nesse meio-tempo.
            if (!profissionalAindaAtende(entry.getProfessional(), dateTime)) {
                entry.setStatus(WaitlistStatus.CANCELADA);
                waitlistEntryRepository.save(entry);
                proximo = waitlistEntryRepository
                    .findFirstByProfessionalIdAndDateTimeAndStatusOrderByCriadoEmAsc(professionalId, dateTime, WaitlistStatus.AGUARDANDO);
                continue;
            }

            var appointment = new Appointment(
                entry.getProfessional(), paciente, dateTime,
                "Vaga da lista de espera", StatusConsulta.AGUARDANDO_CONFIRMACAO);
            appointmentRepository.save(appointment);

            entry.setAppointment(appointment);
            entry.setStatus(WaitlistStatus.OFERECIDA);
            entry.setToken(UUID.randomUUID().toString());
            entry.setOfertaExpiraEm(prazoDeOferta(dateTime));
            waitlistEntryRepository.save(entry);

            emailTemplateService.notificarVagaDisponivel(entry);
            // Igual a qualquer agendamento normal (agendar()): o profissional também precisa
            // ser avisado de que há uma solicitação nova aguardando aprovação dele.
            emailTemplateService.notificarSolicitacaoAgendamentoParaMedico(appointment);
            return;
        }
    }

    private boolean profissionalAindaAtende(Professional professional, LocalDateTime dateTime) {
        LocalTime horario = dateTime.toLocalTime().withMinute(0).withSecond(0).withNano(0);
        return availabilityRepository.findByProfessionalEmailAndDate(professional.getEmail(), dateTime.toLocalDate())
            .stream()
            .anyMatch(a -> a.getStartTime().equals(horario));
    }

    // O prazo de confirmação nunca ultrapassa o próprio horário da consulta — evita uma oferta
    // de 2h (padrão) sobreviver a um cancelamento de última hora que libera um horário daqui a
    // poucos minutos.
    private LocalDateTime prazoDeOferta(LocalDateTime dateTime) {
        var prazoPadrao = LocalDateTime.now().plusHours(horasOferta);
        return dateTime.isBefore(prazoPadrao) ? dateTime : prazoPadrao;
    }

    // ─── Confirmação da oferta (link público, token de uso único) ──────────────

    @Transactional(readOnly = true)
    public WaitlistOfertaStatusDTO consultarOferta(String token) {
        var entry = waitlistEntryRepository.findByToken(token).orElse(null);
        if (entry == null) {
            return new WaitlistOfertaStatusDTO(false, false, false, null, null);
        }

        return new WaitlistOfertaStatusDTO(
            true,
            entry.getStatus() == WaitlistStatus.CONFIRMADA,
            entry.getStatus() == WaitlistStatus.EXPIRADA || entry.isOfertaExpirada(),
            entry.getProfessional().getName(),
            entry.getDateTime().format(FMT)
        );
    }

    @Transactional
    public void confirmarOferta(String token) {
        var entry = waitlistEntryRepository.findByToken(token)
            .orElseThrow(() -> new RuntimeException("Oferta inválida."));

        if (entry.getStatus() == WaitlistStatus.CONFIRMADA) {
            throw new RuntimeException("Você já confirmou esta vaga.");
        }
        if (entry.getStatus() != WaitlistStatus.OFERECIDA) {
            throw new RuntimeException("Esta oferta não está mais disponível.");
        }
        if (entry.isOfertaExpirada()) {
            throw new RuntimeException("O prazo para confirmar esta vaga expirou.");
        }

        entry.setStatus(WaitlistStatus.CONFIRMADA);
        try {
            waitlistEntryRepository.save(entry);
        } catch (org.springframework.dao.OptimisticLockingFailureException e) {
            // Corrida rara: o scheduler (a cada 15min) processou esta mesma oferta como vencida
            // no mesmíssimo instante — quem confirmar primeiro no banco vence, o outro cai aqui.
            throw new RuntimeException("O prazo para confirmar esta vaga expirou.");
        }

        auditLogService.log(entry.getPatient().getEmail(), "CONFIRMACAO_VAGA_LISTA_ESPERA", "WaitlistEntry", entry.getId(),
            "Paciente confirmou a vaga via link público (token).");
    }

    // ─── Expiração automática (chamado pelo WaitlistExpirationScheduler) ───────

    @Transactional
    public void expirarOfertasVencidas() {
        var vencidas = waitlistEntryRepository.findByStatusAndOfertaExpiraEmBefore(WaitlistStatus.OFERECIDA, LocalDateTime.now());

        for (var entry : vencidas) {
            try {
                var appointment = entry.getAppointment();

                if (appointment.getStatus() != StatusConsulta.AGUARDANDO_CONFIRMACAO) {
                    // Paciente/profissional já avançaram pelo fluxo normal do painel (aprovou,
                    // confirmou...) sem passar pelo link do e-mail — a reserva "pegou" mesmo
                    // assim. Não cancelar uma consulta que já andou; só sincroniza a entrada.
                    entry.setStatus(WaitlistStatus.CONFIRMADA);
                    waitlistEntryRepository.save(entry);
                    continue;
                }

                // Marca EXPIRADA e salva ANTES de cancelar a consulta de verdade — de propósito.
                // Se o paciente confirmou a oferta bem nesse instante (confirmarOferta já
                // comitou CONFIRMADA), o @Version em WaitlistEntry faz este save falhar com
                // OptimisticLockingFailureException aqui, ANTES de qualquer coisa irreversível
                // acontecer — a consulta nunca chega a ser cancelada por engano.
                entry.setStatus(WaitlistStatus.EXPIRADA);
                waitlistEntryRepository.save(entry);

                appointmentService.cancelarPorExpiracaoDeOferta(appointment.getId());
                self.ofertarProximoDaFila(entry.getProfessional().getId(), entry.getDateTime());
            } catch (Exception e) {
                logger.warn("Falha ao expirar oferta de lista de espera {}: {}", entry.getId(), e.getMessage());
            }
        }
    }
}
