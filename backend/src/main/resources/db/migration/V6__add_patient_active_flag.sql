-- LGPD: flag de soft-delete por anonimização. Pacientes anonimizados (is_active = false)
-- continuam no banco para manter a integridade referencial de consultas e prontuários
-- (CFM exige retenção de prontuários por 20 anos), mas seus dados pessoais são
-- substituídos por valores irreversíveis no momento da exclusão.

ALTER TABLE patient ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;
