-- E7 (Penalidades por Falta/Cancelamento Tardio): coluna que marca até quando o paciente
-- está temporariamente bloqueado para novos agendamentos (null = não bloqueado).
--
-- A coluna já existia em produção, criada por ddl-auto=update em versões antigas — nunca teve
-- migration própria (drift de schema não rastreável). Esta migration formaliza a definição para
-- ambientes onde o Flyway é a única autoridade de schema (prod atual roda ddl-auto=none).
-- IF NOT EXISTS torna o passo idempotente: no-op onde a coluna já está, cria onde falta.

ALTER TABLE patient ADD COLUMN IF NOT EXISTS blocked_until TIMESTAMP;
