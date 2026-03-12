package com.sistema.lucas.repository;

import com.sistema.lucas.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByProfessionalId(Long professionalId);
    List<Appointment> findByPatientId(Long patientId);
}