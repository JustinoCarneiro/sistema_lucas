---
tipo: decisao
data: 2026-05-21
status: Superada (27/08/2026) — fluxo de MFA implementado, ver CLAUDE.md
---

# MFA/TOTP: schema preparado, fluxo de login ainda não usa

> ⚠️ **Superada em 27/08/2026:** o fluxo completo de MFA (login em duas etapas, ADMIN/
> PROFESSIONAL/PATIENT, `MfaController`/`MfaService`/`TotpService`) foi implementado — ver seção
> de Princípios do `CLAUDE.md`. Esta nota fica só como histórico de por que o schema existia
> sozinho por tanto tempo; não reflete mais o estado atual do sistema.

## Contexto
Migration `V10__add_mfa_columns.sql` adiciona `users.mfa_enabled` e `users.totp_secret` (este último já protegido por `EncryptionConverter`), mas nenhum fluxo de autenticação (`login`, `AuthController`) lê ou valida esses campos hoje.

## Decisão
Preparar a estrutura de dados adiantada, deliberadamente, **sem** alterar o fluxo de login nesta etapa — confirmado no próprio comentário da migration ("Nesta etapa, o fluxo de login NÃO é alterado — apenas a estrutura de dados é preparada"). Não é um recurso esquecido pela metade por falta de tempo; é uma decisão consciente de sequenciamento.

## Consequências
- Não assumir que MFA está ativo em nenhum contexto (auditoria de segurança, resposta a incidente, etc.) — `mfa_enabled=true` num usuário, se existir, não significa que o login realmente exige TOTP.
- Se/quando o fluxo de MFA for implementado de fato, ele precisa nascer como uma história nova no ÉPICO 1 (Autenticação) — o schema já existe, não recriar as colunas.
- Ao investigar qualquer bug relacionado a login, lembrar que TOTP não está no caminho crítico ainda, mesmo que o campo apareça no modelo de dados.
