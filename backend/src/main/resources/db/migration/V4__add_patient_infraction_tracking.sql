-- Adiciona colunas para controle de faltas e advertências no paciente
ALTER TABLE patient ADD COLUMN infraction_count INT DEFAULT 0;
ALTER TABLE patient ADD COLUMN received_first_warning BOOLEAN DEFAULT FALSE;
