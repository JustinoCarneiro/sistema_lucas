// backend/src/main/java/com/sistema/lucas/service/NpsService.java
package com.sistema.lucas.service;

import com.sistema.lucas.model.Appointment;
import com.sistema.lucas.model.NpsResponse;
import com.sistema.lucas.model.dto.NpsStatusDTO;
import com.sistema.lucas.repository.NpsResponseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class NpsService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    @Autowired private NpsResponseRepository npsResponseRepository;
    @Autowired private EmailTemplateService emailTemplateService;

    // Chamado de dentro da transação de ProntuarioService.create(). REQUIRES_NEW
    // isola numa transação própria: se algo falhar aqui, só esse pedido de NPS é
    // desfeito — não marca a transação da conclusão da consulta como rollback-only
    // (o que aconteceria com a propagação padrão, mesmo com try/catch no chamador).
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void solicitarAvaliacao(Appointment appointment) {
        if (npsResponseRepository.findByAppointmentId(appointment.getId()).isPresent()) {
            return; // já existe solicitação pra essa consulta — idempotente
        }

        var nps = new NpsResponse();
        nps.setAppointment(appointment);
        nps.setPatient(appointment.getPatient());
        nps.setToken(UUID.randomUUID().toString());
        nps.setExpiraEm(LocalDateTime.now().plusDays(7));
        npsResponseRepository.save(nps);

        emailTemplateService.solicitarAvaliacaoNps(appointment, nps.getToken());
    }

    @Transactional(readOnly = true)
    public NpsStatusDTO consultarStatus(String token) {
        var nps = npsResponseRepository.findByToken(token).orElse(null);
        if (nps == null) {
            return new NpsStatusDTO(false, false, false, null, null);
        }

        return new NpsStatusDTO(
            true,
            nps.isRespondido(),
            nps.isExpirado(),
            nps.getAppointment().getProfessional().getName(),
            nps.getAppointment().getDateTime().format(FMT)
        );
    }

    @Transactional
    public void responder(String token, int score, String comentario) {
        var nps = npsResponseRepository.findByToken(token)
            .orElseThrow(() -> new RuntimeException("Link de avaliação inválido."));

        if (nps.isRespondido()) {
            throw new RuntimeException("Esta consulta já foi avaliada.");
        }
        if (nps.isExpirado()) {
            throw new RuntimeException("O prazo para avaliar esta consulta expirou.");
        }
        if (score < 0 || score > 10) {
            throw new RuntimeException("A nota deve estar entre 0 e 10.");
        }

        nps.setScore(score);
        nps.setComentario(comentario);
        nps.setRespondidoEm(LocalDateTime.now());
        npsResponseRepository.save(nps);
    }
}
