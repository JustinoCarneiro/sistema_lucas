-- backend/src/main/resources/db/migration/V3__add_unique_constraint_appointments.sql
-- Garante que um profissional não pode ter dois agendamentos no mesmo horário
-- (Impede race conditions onde dois pacientes tentam agendar o mesmo slot simultaneamente)

ALTER TABLE appointments
ADD CONSTRAINT uk_professional_datetime UNIQUE (professional_id, date_time);
