// backend/src/main/java/com/sistema/lucas/repository/AppointmentRepository.java
package com.sistema.lucas.repository;

import com.sistema.lucas.model.Appointment;
import com.sistema.lucas.model.enums.StatusConsulta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByProfessionalId(Long professionalId);

    List<Appointment> findByPatientId(Long patientId);

    List<Appointment> findByPatientEmail(String email);

    List<Appointment> findByProfessionalEmail(String email);

    // Agenda de hoje do profissional
    @Query("SELECT a FROM Appointment a WHERE a.professional.email = :email AND CAST(a.dateTime AS date) = CURRENT_DATE")
    List<Appointment> findTodayAppointmentsByProfessionalEmail(@Param("email") String email);

    // Próximas consultas do profissional (status AGENDADA, a partir de agora)
    @Query("SELECT a FROM Appointment a WHERE a.professional.email = :email AND a.dateTime > :agora AND a.status = 'AGENDADA' ORDER BY a.dateTime ASC")
    List<Appointment> findProximasByProfissionalEmail(
        @Param("email") String email,
        @Param("agora") LocalDateTime agora
    );

    // Total de pacientes únicos atendidos pelo profissional
    @Query("SELECT COUNT(DISTINCT a.patient.id) FROM Appointment a WHERE a.professional.email = :email")
    Long countPacientesUnicosByProfissional(@Param("email") String email);

    // Próxima consulta agendada do paciente
    @Query("SELECT a FROM Appointment a WHERE a.patient.email = :email AND a.dateTime > :agora AND a.status = 'AGENDADA' ORDER BY a.dateTime ASC")
    List<Appointment> findProximaByPacienteEmail(
        @Param("email") String email,
        @Param("agora") LocalDateTime agora
    );

    // Total de consultas por status para um paciente
    Long countByPatientEmailAndStatus(String email, StatusConsulta status);

    // Consultas em um intervalo de datas (dashboard admin)
    @Query("SELECT a FROM Appointment a WHERE a.dateTime >= :inicio AND a.dateTime < :fim")
    List<Appointment> countByDateRange(
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim
    );

    // Total por status (dashboard admin)
    Long countByStatus(StatusConsulta status);

    // ✅ Consultas para lembrete — agendadas/confirmadas no intervalo informado
    @Query("SELECT a FROM Appointment a WHERE a.dateTime >= :inicio AND a.dateTime < :fim AND a.status IN ('AGENDADA', 'CONFIRMADA_PROFISSIONAL', 'CONFIRMADA')")
    List<Appointment> findConsultasParaLembrete(
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim
    );

    // ✅ Buscar consultas de um profissional em um intervalo, excluindo canceladas (para validar conflitos)
    @Query("SELECT a FROM Appointment a WHERE a.professional.id = :profId AND a.dateTime >= :inicio AND a.dateTime < :fim AND a.status <> :statusExcluido")
    List<Appointment> findByProfessionalIdAndDateTimeBetweenAndStatusNot(
        @Param("profId") Long professionalId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim,
        @Param("statusExcluido") StatusConsulta status
    );
}