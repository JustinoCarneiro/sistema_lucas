-- backend/src/main/resources/db/migration/V21__create_mfa_backup_codes.sql
-- MFA (SEC-02): códigos de backup de uso único, gerados no momento em que o usuário ativa o
-- TOTP. Só o hash HMAC-SHA256 fica em repouso (mesmo padrão de cpf_hash) — o código em texto
-- plano só existe na resposta HTTP do /auth/mfa/enable, uma única vez.
CREATE TABLE mfa_backup_codes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    code_hash VARCHAR(255) NOT NULL,
    used_at TIMESTAMP,
    criado_em TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_mfa_backup_codes_user_id ON mfa_backup_codes (user_id);
