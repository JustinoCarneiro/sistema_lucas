# Sistema Lucas - Contrato canônico de trabalho

## Objetivo

Plataforma de prontuário eletrônico e agendamento de consultas para clínica,
com dado sensível de saúde e alto peso de conformidade LGPD. Em produção; Fase 5
da metodologia OndaDev (manutenção e evolução). Versão adotada: `ONDA_VERSION`.

## Mapa do repositório

| Caminho | Finalidade |
| --- | --- |
| `CLAUDE.md` | Espec Viva: stack, princípios não-funcionais, épicos, máquina de estados, convenções. |
| `docs/spec.md` | Histórias de usuário completas e critérios de aceite BDD. |
| `ROADMAP.md` | Blueprint técnico: módulos, pesos, contratos. |
| `backend/` | Spring Boot 3.4 + Java 21 (Maven); migrations Flyway. |
| `frontend/` | Angular 21 (standalone, Signals) + Tailwind CSS v4. |
| `docs/compliance/` | Auditoria LGPD, DPO, plano de resposta a incidentes. |
| `memoria-tecnica/` | Bugs cabeludos e decisões fora da spec; consulte antes de investigar. |
| `design/` | `tokens.css` + `DESIGN.md`. |
| `scripts/jira_sync.py` | Sincronização pontual de issues no board Jira `LUC`; nunca automática. |
| `.agents/`, `.claude/` | Skills dos agentes. |
| `docker-compose*.yml` | PostgreSQL, backend e frontend locais. |

## Autoridade da informação

| Assunto | Fonte canônica | Papel das demais fontes |
| --- | --- | --- |
| Escopo, histórias e aceite | `CLAUDE.md` + `docs/spec.md` | Jira (board `LUC`) apenas reflete o status. |
| Ordem técnica e progresso | `ROADMAP.md` | Jira é projeção visual. |
| Decisão de arquitetura | `memoria-tecnica/decisoes/` | — |
| Dados de saúde e LGPD | `CLAUDE.md` (Princípios) + `docs/compliance/` | Nenhuma tarefa pode contrariar. |
| Código e histórico versionado | Git | GitHub registra PRs, revisão e CI. |
| Trabalho externo | Jira/GitHub | Nunca sobrescreve a verdade local sem decisão explícita. |

Jira é uma projeção do status, nunca o bloqueio da edição local. A spec muda
primeiro nos arquivos; o board `LUC` é acertado depois, à mão na UI ou com
`scripts/jira_sync.py` para lotes pontuais — **nunca disparado automaticamente
por edição de doc**. Exclusão de issue exige confirmação explícita.

## Comandos verificados

```bash
# Backend (Java 21 + Maven; sobe PostgreSQL de teste)
cd backend && ./mvnw verify

# Frontend (Angular 21)
cd frontend && npm ci
cd frontend && npm test               # Vitest (unit)
cd frontend && npm run cypress:run    # Cypress E2E (precisa dos containers de pé)

# Ambiente local
docker compose up -d
docker compose config
```

## Fronteiras e convenções

- **Diretiva Primária:** não altere a sintaxe ou o comportamento de código
  existente sem um teste que justifique a quebra (ciclo TDD).
- Erros via `GlobalExceptionHandler` (`@RestControllerAdvice`) → `ExceptionDTO(message, code)`.
- DTOs como Java Records; controller nunca retorna `@Entity`.
- `snake_case` no banco, `camelCase` no Java/TypeScript.
- Migrations Flyway só para a frente; nunca editar uma já aplicada.
- Rotas sem prefixo `/api`/`/v1` (decisão de 27/08/2026 — sistema interno).
- Documentação em português claro; nomes técnicos no idioma da tecnologia.
- Consulte `memoria-tecnica/bugs/` antes de investigar bug não trivial e
  `memoria-tecnica/decisoes/` antes de decidir arquitetura.

## Segurança e classes de risco

Dado de saúde e identificação de paciente é PII sensível sob LGPD. Campo sensível
nunca em texto plano em repouso (AES-256-GCM por campo); CPF só por `cpf_hash`
(HMAC-SHA256 com pepper); exclusão de paciente com vínculo clínico é
anonimização, não DELETE (retenção CFM 20 anos); toda leitura/escrita de dado
sensível é auditada. Nunca versione, exiba em log ou cole em prompt: tokens,
chaves, pepper, senhas, dados de paciente reais ou exports. Use `.env` local
(`.env`, `.env.dev`, `.env.jira` não são versionados; segredos de produção em
`secrets/`, também fora do Git).

| Nível | Exemplos | Regra |
| --- | --- | --- |
| R0 | Leitura, docs, testes locais | Executar e validar normalmente. |
| R1 | Código, dependência, schema, migration, CI, configuração compartilhada | Declarar impacto, testar e pedir revisão de diff. |
| R2 | Produção, auth/MFA, criptografia, dados de paciente, credenciais, deploy, exclusão | Exigir autorização explícita e alvo confirmado. Plano + revisão cruzada. |

## Definition of Done

1. atende a uma história de `docs/spec.md` ou escopo escrito com critérios verificáveis;
2. executa os testes que existem (`backend ./mvnw verify`; `frontend npm test` +
   Cypress) e reporta o resultado;
3. atualiza `CLAUDE.md`, `docs/spec.md`, `ROADMAP.md` ou `memoria-tecnica/`
   quando o contrato mudou;
4. não introduz segredo, credencial, pepper ou dado de paciente no repositório;
5. passa por revisão proporcional ao risco e deixa um diff compreensível;
6. registra handoff com mudanças, validações, decisões, riscos e pendências.

Não afirme que testes, CI, deploy ou sincronização passaram sem evidência.

## Revisão e handoff entre agentes

Claude e Codex seguem este arquivo como núcleo comum. Um autor por PR; o outro
revisa o diff quando o risco (R1/R2) exige, com o mínimo suficiente (contrato,
diff, logs de teste). Quando a cota de um agente acaba, o outro assume por
handoff — protocolo na metodologia OndaDev 3.0 (`ONDA_VERSION`).

Síntese de handoff:

```text
Escopo: …
Mudanças: …
Validações executadas e resultado: …
Decisões/ADRs: …
Riscos, bloqueios e próximos passos: …
```
