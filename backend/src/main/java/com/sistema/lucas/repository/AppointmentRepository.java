package com.sistema.lucas.repository;

import com.sistema.lucas.domain.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("""
        SELECT COUNT(a) > 0 
        FROM Appointment a 
        WHERE a.doctor.id = :doctorId 
        AND a.status <> 'CANCELLED' 
        AND (a.startTime < :endTime AND a.endTime > :startTime)
    """)
    boolean existsConflict(Long doctorId, LocalDateTime startTime, LocalDateTime endTime);

    // Busca para o painel do Paciente
    Page<Appointment> findAllByPatientId(Long patientId, Pageable pageable);

    // NOVA: Busca para o painel do Médico (Retorna lista ou página, aqui usamos Lista para simplificar o dashboard)
    List<Appointment> findByDoctorIdOrderByStartTimeAsc(Long doctorId);
}