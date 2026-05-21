// backend/src/main/java/com/sistema/lucas/model/dto/AppointmentResponseDTO.java
package com.sistema.lucas.model.dto;

import com.sistema.lucas.model.Appointment;
import com.sistema.lucas.model.enums.ModalidadeAtendimento;
import com.sistema.lucas.model.enums.StatusConsulta;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record AppointmentResponseDTO(
    @JsonProperty("id") Long id,
    @JsonProperty("patientId") Long patientId,
    @JsonProperty("professionalName") String professionalName,
    @JsonProperty("patientName") String patientName,
    @JsonProperty("startTime") LocalDateTime startTime,
    @JsonProperty("reason") String reason,
    @JsonProperty("cancelReason") String cancelReason,
    @JsonProperty("status") StatusConsulta status,
    @JsonProperty("podeCancelar") boolean podeCancelar,
    @JsonProperty("atrasada") boolean atrasada,
    @JsonProperty("modalidadeAtendimento") String modalidadeAtendimento
) {
    public AppointmentResponseDTO(Appointment app) {
        this(
            app.getId(),
            app.getPatient().getId(),
            app.getProfessional().getName(),
            app.getPatient().getName(),
            app.getDateTime(),
            app.getReason(),
            app.getCancelReason(),
            app.getStatus(),
            (app.getStatus() == StatusConsulta.AGUARDANDO_CONFIRMACAO
                    || app.getStatus() == StatusConsulta.AGENDADA
                    || app.getStatus() == StatusConsulta.CONFIRMADA_PROFISSIONAL
                    || app.getStatus() == StatusConsulta.CONFIRMADA),
            java.time.LocalDateTime.now().isAfter(app.getDateTime().minusHours(24)),
            app.getProfessional().getModalidadeAtendimento() != null
                ? app.getProfessional().getModalidadeAtendimento().name()
                : ModalidadeAtendimento.PRESENCIAL.name()
        );
    }
}
