// backend/src/main/java/com/sistema/lucas/model/dto/AppointmentResponseDTO.java
package com.sistema.lucas.model.dto; 

import com.sistema.lucas.model.Appointment;
import java.time.LocalDateTime; 

public record AppointmentResponseDTO( 
    Long id, 
    String professionalName, 
    String patientName, 
    LocalDateTime startTime, // Renomeado para 'startTime' para bater com o HTML
    String reason, 
    String status 
) { 
    public AppointmentResponseDTO(Appointment app) { 
        this( 
            app.getId(), 
            app.getProfessional().getName(), 
            app.getPatient().getName(), 
            app.getDateTime(), // Pega o valor da entidade
            app.getReason(), 
            app.getStatus() 
        ); 
    } 
}