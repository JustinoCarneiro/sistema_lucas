package com.sistema.lucas.model.dto; 

// ADICIONE ESTA LINHA:
import com.sistema.lucas.model.Appointment;
import java.time.LocalDateTime; 

public record AppointmentResponseDTO( 
    Long id, 
    String professionalName, 
    String patientName, 
    LocalDateTime dateTime, 
    String reason, 
    String status 
) { 
    public AppointmentResponseDTO(Appointment app) { 
        this( 
            app.getId(), 
            app.getProfessional().getName(), 
            app.getPatient().getName(), 
            app.getDateTime(), 
            app.getReason(), 
            app.getStatus() 
        ); 
    } 
}