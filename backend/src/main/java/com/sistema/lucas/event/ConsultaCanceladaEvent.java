// backend/src/main/java/com/sistema/lucas/event/ConsultaCanceladaEvent.java
package com.sistema.lucas.event;

import java.time.LocalDateTime;

// Publicado por AppointmentService.cancelar() e consumido por WaitlistService (M13) — evento
// em vez de injeção direta pra evitar dependência circular entre os dois serviços
// (AppointmentService <-> WaitlistService), que o Spring Boot recusa resolver por padrão.
public record ConsultaCanceladaEvent(Long professionalId, LocalDateTime dateTime) {}
