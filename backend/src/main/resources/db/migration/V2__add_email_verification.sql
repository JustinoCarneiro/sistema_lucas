-- V2__add_email_verification.sql
-- Adiciona campo de verificação na tabela de usuários
ALTER TABLE users ADD COLUMN verified BOOLEAN DEFAULT FALSE;

-- Cria tabela para tokens de verificação
CREATE TABLE verification_tokens (
    id SERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
