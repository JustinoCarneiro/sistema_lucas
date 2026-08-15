// backend/src/main/java/com/sistema/lucas/model/dto/WaitlistEntryResponseDTO.java
package com.sistema.lucas.model.dto;

import com.sistema.lucas.model.WaitlistEntry;
import com.sistema.lucas.model.enums.WaitlistStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record WaitlistEntryResponseDTO(
    @JsonProperty("id") Long id,
    @JsonProperty("professionalName") String professionalName,
    @JsonProperty("dateTime") LocalDateTime dateTime,
    @JsonProperty("status") WaitlistStatus status,
    @JsonProperty("criadoEm") LocalDateTime criadoEm
) {
    public WaitlistEntryResponseDTO(WaitlistEntry entry) {
        this(
            entry.getId(),
            entry.getProfessional().getName(),
            entry.getDateTime(),
            entry.getStatus(),
            entry.getCriadoEm()
        );
    }
}
