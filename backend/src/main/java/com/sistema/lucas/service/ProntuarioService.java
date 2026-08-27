// backend/src/main/java/com/sistema/lucas/service/ProntuarioService.java
package com.sistema.lucas.service;

import com.sistema.lucas.model.Prontuario;
import com.sistema.lucas.model.enums.Role;
import com.sistema.lucas.model.enums.StatusConsulta;
import com.sistema.lucas.repository.AppointmentRepository;
import com.sistema.lucas.repository.ProntuarioRepository;
import com.sistema.lucas.repository.ProfessionalRepository;
import com.sistema.lucas.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ProntuarioService {

    private static final Logger logger = LoggerFactory.getLogger(ProntuarioService.class);

    @Autowired private ProntuarioRepository prontuarioRepository;
    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private ProfessionalRepository professionalRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AuditLogService auditLogService;
    @Autowired private NpsService npsService;

    public List<Prontuario> getByPatientId(Long patientId, String userEmail) {
        // ADMIN vê qualquer paciente; PROFESSIONAL só pode ver prontuário de paciente que já
        // atendeu — evita acesso irrestrito a histórico clínico de terceiros (IDOR).
        var solicitante = userRepository.findByEmail(userEmail);
        boolean isAdmin = solicitante != null && solicitante.getRole() == Role.ADMIN;
        if (!isAdmin && !appointmentRepository.existsByProfessionalEmailAndPatientId(userEmail, patientId)) {
            throw new RuntimeException("Operação de Segurança: Acesso negado. Você nunca atendeu este paciente.");
        }

        auditLogService.log(userEmail, "VISUALIZACAO", "Prontuario", patientId, "Visualizou histórico de prontuários do paciente ID: " + patientId);
        return prontuarioRepository.findByPatientIdOrderByCriadoEmDesc(patientId);
    }

    @Transactional
    public Prontuario create(@org.springframework.lang.NonNull Long appointmentId, String notas, String professionalEmail) {
        var appointment = appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> new RuntimeException("Consulta não encontrada"));

        // 🛡️ Segurança (IDOR): só o profissional dono da consulta pode concluí-la, igual ao
        // resto de AppointmentService (aprovarAgendamento/recusarAgendamento/marcarFalta).
        if (!appointment.getProfessional().getEmail().equals(professionalEmail)) {
            throw new RuntimeException("Operação de Segurança: Você não é o profissional dessa consulta.");
        }

        if (notas == null || notas.isBlank()) {
            throw new RuntimeException("As notas do prontuário são obrigatórias.");
        }

        // Idempotência: evita duplo clique/retry criando dois prontuários pra mesma consulta.
        if (prontuarioRepository.existsByAppointmentId(appointmentId)) {
            throw new RuntimeException("Esta consulta já tem um prontuário registrado.");
        }

        var professional = professionalRepository.findByEmail(professionalEmail)
            .orElseThrow(() -> new RuntimeException("Profissional não encontrado"));

        appointment.setStatus(StatusConsulta.CONCLUIDA);
        appointmentRepository.save(appointment);

        var prontuario = new Prontuario();
        prontuario.setAppointment(appointment);
        prontuario.setPatient(appointment.getPatient());
        prontuario.setProfessional(professional);
        prontuario.setNotas(notas);

        Prontuario saved;
        try {
            saved = prontuarioRepository.save(prontuario);
            // flush força o INSERT a rodar agora — sem isso, a violação da constraint única
            // (uk_prontuario_appointment, corrida que passou pela checagem existsByAppointmentId
            // acima) só apareceria no commit, depois deste método já ter retornado.
            prontuarioRepository.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new RuntimeException("Esta consulta já tem um prontuário registrado.");
        }
        auditLogService.log(professionalEmail, "CRIACAO", "Prontuario", saved.getId(), "Criou prontuário para consulta ID: " + appointmentId);

        // NPS (M11): efeito colateral não-crítico — nunca deve impedir a conclusão da consulta.
        try {
            npsService.solicitarAvaliacao(appointment);
        } catch (Exception e) {
            logger.warn("Falha ao solicitar avaliação NPS para consulta {}: {}", appointmentId, e.getMessage());
        }

        return saved;
    }
}