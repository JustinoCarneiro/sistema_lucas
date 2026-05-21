-- V11: Criação de tabelas para gerenciar Refresh Tokens rotativos e Denylist de JWTs

CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMP
);

CREATE TABLE token_denylist (
    token VARCHAR(500) PRIMARY KEY,
    expires_at TIMESTAMP NOT NULL
);
