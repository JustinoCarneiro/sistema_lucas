-- Torna o campo CPF original obrigatório no banco
ALTER TABLE patient ALTER COLUMN cpf SET NOT NULL;

-- Adiciona coluna para o hash SHA-256 do CPF (64 caracteres hexadecimais)
ALTER TABLE patient ADD COLUMN cpf_hash VARCHAR(64);

-- NOTA: Em produção com dados existentes, calcule os hashes antes de aplicar as constraints.
-- Para base limpa de desenvolvimento:
ALTER TABLE patient ADD CONSTRAINT unique_cpf_hash UNIQUE (cpf_hash);
ALTER TABLE patient ALTER COLUMN cpf_hash SET NOT NULL;
