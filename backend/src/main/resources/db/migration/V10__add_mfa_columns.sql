-- SEC-02: Fundação para Multi-Factor Authentication (MFA) no painel administrativo.
-- Adiciona colunas para suportar TOTP (Time-based One-Time Password).
-- Nesta etapa, o fluxo de login NÃO é alterado — apenas a estrutura de dados é preparada.

ALTER TABLE users ADD COLUMN IF NOT EXISTS mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS totp_secret VARCHAR(255);
