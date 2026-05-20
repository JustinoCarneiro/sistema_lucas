-- Torna o campo CPF original obrigatório no banco
ALTER TABLE patient ALTER COLUMN cpf SET NOT NULL;

-- Adiciona coluna para o hash SHA-256 do CPF (64 caracteres hexadecimais)
ALTER TABLE patient ADD COLUMN cpf_hash VARCHAR(64);

-- Backfill para bases com pacientes pré-existentes: calcula o SHA-256 do CPF
-- (apenas dígitos) replicando exatamente Patient.gerarCpfHash da aplicação,
-- para que as constraints abaixo possam ser aplicadas sem violar NOT NULL.
CREATE EXTENSION IF NOT EXISTS pgcrypto;
UPDATE patient
SET cpf_hash = encode(digest(regexp_replace(cpf, '[^0-9]', '', 'g'), 'sha256'), 'hex')
WHERE cpf_hash IS NULL;

ALTER TABLE patient ADD CONSTRAINT unique_cpf_hash UNIQUE (cpf_hash);
ALTER TABLE patient ALTER COLUMN cpf_hash SET NOT NULL;
