-- backend/src/main/resources/db/migration/V14__fix_appointment_unique_constraint.sql
-- Remove a trava absoluta que impedia reaproveitar horários cancelados
ALTER TABLE appointments DROP CONSTRAINT IF EXISTS uk_professional_datetime;

-- Cria uma nova trava (índice único) que ignora os agendamentos cancelados,
-- permitindo que o horário volte a ficar disponível se a consulta for desmarcada ou recusada.
CREATE UNIQUE INDEX idx_appointments_prof_date_active 
ON appointments (professional_id, date_time) 
WHERE status != 'CANCELADA';
