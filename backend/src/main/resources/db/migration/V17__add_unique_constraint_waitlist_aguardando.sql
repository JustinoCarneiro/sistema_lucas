-- M13 (E4/US-4.8) — fecha uma corrida (check-then-act em WaitlistService.entrarNaFila): dois
-- cliques/abas do mesmo paciente quase simultâneos podiam criar duas entradas AGUARDANDO para
-- o mesmo profissional+horário. Mesmo padrão de índice único parcial já usado em
-- V14__fix_appointment_unique_constraint.sql para appointments.
CREATE UNIQUE INDEX idx_waitlist_unique_aguardando
ON waitlist_entries (patient_id, professional_id, date_time)
WHERE status = 'AGUARDANDO';
