// backend/src/main/java/com/sistema/lucas/event/ConsultaCanceladaEvent.java
package com.sistema.lucas.event;

import java.time.LocalDateTime;

// Publicado por AppointmentService (cancelar/recusarAgendamento/reagendar) e consumido por
// WaitlistService (M13) — evento em vez de injeção direta pra evitar dependência circular entre
// os dois serviços (AppointmentService <-> WaitlistService), que o Spring Boot recusa resolver
// por padrão. appointmentId identifica a consulta que liberou (ou moveu-se de) o horário —
// usado pra sincronizar um WaitlistEntry vinculado a ela, se existir.
public record ConsultaCanceladaEvent(Long appointmentId, Long professionalId, LocalDateTime dateTime) {}
