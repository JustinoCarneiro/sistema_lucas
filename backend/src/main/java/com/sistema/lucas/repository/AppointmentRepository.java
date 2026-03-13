package com.sistema.lucas.repository;

import com.sistema.lucas.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // ADICIONADO
import org.springframework.data.repository.query.Param; // ADICIONADO
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    
    List<Appointment> findByProfessionalId(Long professionalId);
    
    List<Appointment> findByPatientId(Long patientId);

    @Query("SELECT a FROM Appointment a WHERE a.professional.email = :email AND CAST(a.dateTime AS date) = CURRENT_DATE")
    List<Appointment> findTodayAppointmentsByProfessionalEmail(@Param("email") String email);

    List<Appointment> findByPatientEmail(String email);
}