package com.sistema.lucas.repository;

import com.sistema.lucas.domain.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // A Lógica de Ouro: Detecta sobreposição de horários
    @Query("""
        SELECT COUNT(a) > 0 
        FROM Appointment a 
        WHERE a.doctor.id = :doctorId 
        AND a.status <> 'CANCELLED' 
        AND (a.startTime < :endTime AND a.endTime > :startTime)
    """)
    boolean existsConflict(Long doctorId, LocalDateTime startTime, LocalDateTime endTime);
    // ... a sua query de conflito continua em cima ...

    // Nova busca para o painel do Paciente!
    org.springframework.data.domain.Page<Appointment> findAllByPatientId(Long patientId, org.springframework.data.domain.Pageable pageable);
}