-- V13: Torna o e-mail case-insensitive no nível do banco.
-- Corrige a classe de bugs de case-sensitivity em login, cadastro público e reset
-- de senha: a aplicação grava o e-mail em minúsculas (@PrePersist), mas várias
-- checagens comparavam o input cru, e o índice UNIQUE era case-sensitive.
--
-- Pré-condição verificada em produção (jun/2026): não existem e-mails que difiram
-- apenas por capitalização — caso existissem, o índice único case-insensitive
-- falharia ao ser recriado nesta conversão.

CREATE EXTENSION IF NOT EXISTS citext;

ALTER TABLE users ALTER COLUMN email TYPE citext;
