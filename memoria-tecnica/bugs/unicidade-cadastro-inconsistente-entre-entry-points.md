---
tipo: bug
data: 2026-06-03
severidade: Alta
status: Resolvido
---

# Unicidade de e-mail/CPF/registro validada de forma inconsistente entre pontos de entrada

## Sintoma
Dois problemas de duplicidade descobertos em auditorias separadas, mesma causa raiz:
1. Login/cadastro/reset de senha eram *case-sensitive* no e-mail — `Usuario@x.com` e `usuario@x.com` eram tratados como contas diferentes.
2. Cadastro público (`AuthController.register`) deixava CPF duplicado cair direto no erro de integridade do banco (mensagem críptica), diferente de `PatientService.create`, que já tinha pré-checagem. `registroConselho` do profissional também permitia duplicata silenciosa por diferença de maiúsculo/minúsculo (`crm123` vs `CRM123`).

## Causa raiz
A regra de negócio "e-mail/CPF/registro são únicos" existia, mas era **reimplementada separadamente** em cada ponto de entrada (`AuthController.register`, `PatientService.create`, `ProfessionalService.create`) — cada um com um nível diferente de rigor, em vez de uma validação centralizada.

## Solução
- `users.email` migrado pra `citext` (V13) — case-insensitive no próprio banco, corrige login/cadastro/reset de uma vez.
- `AuthController.register` ganhou pré-checagem de CPF + `try/catch` no save, igualando ao rigor que `PatientService.create` já tinha.
- `ProfessionalService` normaliza `registroConselho` (trim/uppercase) **antes** de checar duplicidade.
- Checagem de e-mail passou a considerar **todos os usuários** (não só a tabela do próprio papel) — evita cadastrar um profissional com e-mail já usado por um paciente.

**Regra geral:** ao adicionar um novo ponto de entrada de cadastro (novo role, novo fluxo de importação, etc.), replicar TODAS as checagens de unicidade já estabelecidas (e-mail cross-role, CPF via hash, normalização de campos comparáveis) — não assumir que a validação de um Service cobre os outros pontos de entrada.

## Ligado a
- [[teste-time-bomb-lembrete-scheduler]] — achado na mesma leva de auditoria (03/06/2026).
