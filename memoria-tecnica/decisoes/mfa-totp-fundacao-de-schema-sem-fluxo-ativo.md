---
tipo: decisao
data: 2026-05-21
status: Ativa
---

# MFA/TOTP: schema preparado, fluxo de login ainda não usa

## Contexto
Migration `V10__add_mfa_columns.sql` adiciona `users.mfa_enabled` e `users.totp_secret` (este último já protegido por `EncryptionConverter`), mas nenhum fluxo de autenticação (`login`, `AuthController`) lê ou valida esses campos hoje.

## Decisão
Preparar a estrutura de dados adiantada, deliberadamente, **sem** alterar o fluxo de login nesta etapa — confirmado no próprio comentário da migration ("Nesta etapa, o fluxo de login NÃO é alterado — apenas a estrutura de dados é preparada"). Não é um recurso esquecido pela metade por falta de tempo; é uma decisão consciente de sequenciamento.

## Consequências
- Não assumir que MFA está ativo em nenhum contexto (auditoria de segurança, resposta a incidente, etc.) — `mfa_enabled=true` num usuário, se existir, não significa que o login realmente exige TOTP.
- Se/quando o fluxo de MFA for implementado de fato, ele precisa nascer como uma história nova no ÉPICO 1 (Autenticação) — o schema já existe, não recriar as colunas.
- Ao investigar qualquer bug relacionado a login, lembrar que TOTP não está no caminho crítico ainda, mesmo que o campo apareça no modelo de dados.
