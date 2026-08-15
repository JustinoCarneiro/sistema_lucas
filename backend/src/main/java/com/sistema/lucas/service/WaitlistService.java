// backend/src/main/java/com/sistema/lucas/service/WaitlistService.java
package com.sistema.lucas.service;

import com.sistema.lucas.event.ConsultaCanceladaEvent;
import com.sistema.lucas.model.Appointment;
import com.sistema.lucas.model.WaitlistEntry;
import com.sistema.lucas.model.dto.WaitlistEntryResponseDTO;
import com.sistema.lucas.model.dto.WaitlistOfertaStatusDTO;
import com.sistema.lucas.model.enums.StatusConsulta;
import com.sistema.lucas.model.enums.WaitlistStatus;
import com.sistema.lucas.repository.AppointmentRepository;
import com.sistema.lucas.repository.PatientRepository;
import com.sistema.lucas.repository.ProfessionalRepository;
import com.sistema.lucas.repository.WaitlistEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
    @Autowired private PatientRepository patientRepository;
    @Autowired private AppointmentService appointmentService;
    @Autowired private EmailTemplateService emailTemplateService;

    @Value("${app.waitlist.oferta.horas:2}")
    private long horasOferta;

    // ─── Entrar / sair da fila ─────────────────────────────────────────────────

    @Transactional
    public void entrarNaFila(Long professionalId, LocalDateTime dateTime, String emailPaciente) {
        var professional = professionalRepository.findById(professionalId)
            .orElseThrow(() -> new RuntimeException("Profissional não encontrado"));
        var paciente = patientRepository.findByEmail(emailPaciente)
            .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));

        if (!horarioEstaOcupado(professionalId, dateTime)) {
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
    }

    // ─── Reagir ao cancelamento de uma consulta ─────────────────────────────────

    // AFTER_COMMIT: só oferece a vaga depois que o cancelamento realmente foi persistido —
    // evita oferecer um horário que, por algum motivo, acabe não sendo liberado de fato.
    // Escutar um evento (em vez de AppointmentService injetar WaitlistService direto) evita
    // a dependência circular AppointmentService <-> WaitlistService (Spring Boot recusa
    // resolver por padrão). Falha aqui é efeito colateral não-crítico — nunca deve propagar.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoCancelarConsulta(ConsultaCanceladaEvent event) {
        try {
            ofertarProximoDaFila(event.professionalId(), event.dateTime());
        } catch (Exception e) {
            logger.warn("Falha ao ofertar vaga da lista de espera após cancelamento: {}", e.getMessage());
        }
    }

    // ─── Oferecer a vaga ao próximo da fila ─────────────────────────────────────

    // Chamado pelo listener acima (vaga abriu) e por expirarOfertasVencidas() (vaga foi
    // recusada por omissão — cascata pro próximo da fila).
    @Transactional
    public void ofertarProximoDaFila(Long professionalId, LocalDateTime dateTime) {
        var proximo = waitlistEntryRepository
            .findFirstByProfessionalIdAndDateTimeAndStatusOrderByCriadoEmAsc(professionalId, dateTime, WaitlistStatus.AGUARDANDO);

        if (proximo.isEmpty()) return;

        var entry = proximo.get();
        var paciente = entry.getPatient();

        // Paciente bloqueado por penalidade não pode receber a oferta — pula pro próximo da fila.
        if (paciente.getBlockedUntil() != null && paciente.getBlockedUntil().isAfter(LocalDateTime.now())) {
            entry.setStatus(WaitlistStatus.CANCELADA);
            waitlistEntryRepository.save(entry);
            ofertarProximoDaFila(professionalId, dateTime);
            return;
        }

        var appointment = new Appointment(
            entry.getProfessional(), paciente, dateTime,
            "Vaga da lista de espera", StatusConsulta.AGUARDANDO_CONFIRMACAO);
        appointmentRepository.save(appointment);

        entry.setAppointment(appointment);
        entry.setStatus(WaitlistStatus.OFERECIDA);
        entry.setToken(UUID.randomUUID().toString());
        entry.setOfertaExpiraEm(LocalDateTime.now().plusHours(horasOferta));
        waitlistEntryRepository.save(entry);

        emailTemplateService.notificarVagaDisponivel(entry);
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
        waitlistEntryRepository.save(entry);
    }

    // ─── Expiração automática (chamado pelo WaitlistExpirationScheduler) ───────

    @Transactional
    public void expirarOfertasVencidas() {
        var vencidas = waitlistEntryRepository.findByStatusAndOfertaExpiraEmBefore(WaitlistStatus.OFERECIDA, LocalDateTime.now());

        for (var entry : vencidas) {
            try {
                appointmentService.cancelarPorExpiracaoDeOferta(entry.getAppointment().getId());
                entry.setStatus(WaitlistStatus.EXPIRADA);
                waitlistEntryRepository.save(entry);
                ofertarProximoDaFila(entry.getProfessional().getId(), entry.getDateTime());
            } catch (Exception e) {
                logger.warn("Falha ao expirar oferta de lista de espera {}: {}", entry.getId(), e.getMessage());
            }
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private boolean horarioEstaOcupado(Long professionalId, LocalDateTime dateTime) {
        LocalDateTime inicioDia = dateTime.toLocalDate().atStartOfDay();
        LocalDateTime fimDia = dateTime.toLocalDate().plusDays(1).atStartOfDay();
        return appointmentRepository
            .findByProfessionalIdAndDateTimeBetweenAndStatusNot(professionalId, inicioDia, fimDia, StatusConsulta.CANCELADA)
            .stream()
            .anyMatch(a -> a.getDateTime().toLocalTime().equals(dateTime.toLocalTime()));
    }
}
