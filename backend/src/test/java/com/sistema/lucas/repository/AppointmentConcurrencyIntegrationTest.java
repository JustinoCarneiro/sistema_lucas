package com.sistema.lucas.repository;

import com.sistema.lucas.model.Appointment;
import com.sistema.lucas.model.Patient;
import com.sistema.lucas.model.Professional;
import com.sistema.lucas.model.enums.Role;
import com.sistema.lucas.model.enums.StatusConsulta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

// A trava que este teste verifica é um índice único PARCIAL (V14__fix_appointment_unique_
// constraint.sql), não algo expressável via @UniqueConstraint do JPA. Em teste o Flyway fica
// desligado (spring.flyway.enabled=false, ver application.properties de teste) e o schema vem
// só de ddl-auto=create-drop — ou seja, o índice da V14 nunca existe aqui a menos que seja
// recriado manualmente. Sem isso, testUniqueConstraintBlocksSameTime nunca dispara a exceção
// esperada porque a trava simplesmente não existe no banco de teste.
//
// H2 não suporta "CREATE UNIQUE INDEX ... WHERE ..." (índice parcial é um recurso específico
// do Postgres) — reproduzimos a mesma semântica só no H2 de teste com uma coluna computada que
// vira NULL quando CANCELADA (índice único ignora múltiplos NULL, em H2 e Postgres). É uma
// estrutura só de teste — não altera o schema real, que continua sendo o índice parcial da V14.
@DataJpaTest
@Sql(
    statements = {
        // IF NOT EXISTS: ddl-auto=create-drop recria o schema uma vez por classe de teste
        // (contexto Spring), não por método — sem isso, o 2º @Test da classe falharia com
        // "coluna duplicada" ao reexecutar este @Sql antes de cada método.
        "ALTER TABLE appointments ADD COLUMN IF NOT EXISTS test_active_slot_key VARCHAR(100) AS " +
            "(CASE WHEN status <> 'CANCELADA' THEN professional_id || '|' || date_time ELSE NULL END)",
        "CREATE UNIQUE INDEX IF NOT EXISTS idx_appointments_test_active_slot ON appointments (test_active_slot_key)"
    },
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class AppointmentConcurrencyIntegrationTest {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private ProfessionalRepository professionalRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Test
    @DisplayName("Deve lançar exceção ao salvar duas consultas no mesmo horário (Simulação de Concorrência)")
    void testUniqueConstraintBlocksSameTime() {
        Professional professional = new Professional();
        professional.setName("Dr. Teste");
        professional.setEmail("drteste@clinica.com");
        professional.setPassword("senha");
        professional.setRole(Role.PROFESSIONAL);
        professionalRepository.saveAndFlush(professional);

        Patient patient = new Patient();
        patient.setName("Paciente Teste");
        patient.setEmail("paciente@clinica.com");
        patient.setPassword("senha");
        patient.setRole(Role.PATIENT);
        patient.setCpfHash("hash123");
        patientRepository.saveAndFlush(patient);

        LocalDateTime time = LocalDateTime.of(2025, 1, 1, 14, 0);

        Appointment a1 = new Appointment();
        a1.setProfessional(professional);
        a1.setPatient(patient);
        a1.setDateTime(time);
        a1.setStatus(StatusConsulta.AGUARDANDO_CONFIRMACAO);
        appointmentRepository.saveAndFlush(a1);

        Appointment a2 = new Appointment();
        a2.setProfessional(professional);
        a2.setPatient(patient);
        a2.setDateTime(time);
        a2.setStatus(StatusConsulta.AGUARDANDO_CONFIRMACAO);

        assertThrows(DataIntegrityViolationException.class, () -> {
            appointmentRepository.saveAndFlush(a2);
        });
    }

    @Test
    @DisplayName("Deve permitir salvar nova consulta se a anterior no mesmo horário foi CANCELADA")
    void testUniqueConstraintAllowsIfCanceled() {
        Professional professional = new Professional();
        professional.setName("Dra. Teste");
        professional.setEmail("drateste@clinica.com");
        professional.setPassword("senha");
        professional.setRole(Role.PROFESSIONAL);
        professionalRepository.saveAndFlush(professional);

        Patient patient = new Patient();
        patient.setName("Paciente Teste 2");
        patient.setEmail("paciente2@clinica.com");
        patient.setPassword("senha");
        patient.setRole(Role.PATIENT);
        patient.setCpfHash("hash456");
        patientRepository.saveAndFlush(patient);

        LocalDateTime time = LocalDateTime.of(2025, 1, 2, 10, 0);

        Appointment a1 = new Appointment();
        a1.setProfessional(professional);
        a1.setPatient(patient);
        a1.setDateTime(time);
        a1.setStatus(StatusConsulta.CANCELADA); // Simulando uma consulta que foi cancelada
        appointmentRepository.saveAndFlush(a1);

        Appointment a2 = new Appointment();
        a2.setProfessional(professional);
        a2.setPatient(patient);
        a2.setDateTime(time);
        a2.setStatus(StatusConsulta.AGUARDANDO_CONFIRMACAO); // Novo agendamento para o mesmo horário

        assertDoesNotThrow(() -> {
            appointmentRepository.saveAndFlush(a2);
        });
    }
}
