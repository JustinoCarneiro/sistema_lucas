# SISTEMA LUCAS

Plataforma de prontuário eletrônico e agendamento de consultas, com forte foco em conformidade LGPD (dados de saúde e identificação).

## DIRETIVA PRIMÁRIA

> "Leia o `CLAUDE.md` e o `ROADMAP.md`. A partir de agora, não altere a sintaxe do código que eu enviar ou que já existe. Este é o padrão a ser seguido adiante."

## Stack
- **Frontend:** Angular 21 (standalone components, Signals), Tailwind CSS v4 (config CSS-first, sem `tailwind.config`).
- **Backend:** Spring Boot 3.4 · **Java 21** (`pom.xml` alinhado — ver nota abaixo).
- **Banco:** PostgreSQL 15, migrations via Flyway.
- **Auth:** JWT (access token 15min, cookie HttpOnly) + refresh token rotativo (7 dias) + denylist de revogação no logout.
- **Deploy:** Docker Compose (`deploy-dev.sh` / `deploy-prod.sh` / `push-and-deploy.sh` via rsync+SSH).

> ✅ **Divergência de versão reconciliada (27/08/2026):** o `Dockerfile` sempre rodou JDK 21 em produção (`eclipse-temurin:21-jdk-alpine`, build e runtime); só o `pom.xml` mirava bytecode 17 (`<java.version>`/`<release>` desatualizados). `.cursorrules` e `README.md` já diziam 21 corretamente — eram eles que refletiam a intenção real, não o `pom.xml`. Alinhado o `pom.xml` pra 21 (zero risco de runtime, já era o JDK do container) — suíte de backend inteira (164 testes) rodou verde no novo alvo antes de commitar.

## Perfil de projeto
Sistema interno (clínica/consultório) · perfis **ADMIN**, **PROFESSIONAL**, **PATIENT** · dado sensível de saúde, alto peso de conformidade LGPD.

## Princípios (não-funcionais críticos)
- **Toda leitura/escrita de dado sensível é auditada** (`AuditLogService` — `audit_logs`).
- **Campo sensível nunca fica em texto plano em repouso**: CPF, telefone, endereço, contato de emergência, alergias, motivo/justificativa de consulta, conteúdo de prontuário e documento — todos `AES-256-GCM` por campo (`EncryptionConverter`), com suporte a rotação de chave (chaves legadas + fallback AES-128-ECB só para dado histórico pré-migração).
- **CPF nunca é comparado em texto plano** — unicidade via `cpf_hash` (HMAC-SHA256 com pepper secreto), não SHA-256 puro (SHA-256 puro foi a escolha original na V5 e foi considerado fraco contra rainbow table dado o universo limitado de CPFs válidos; `CpfHashBackfillRunner` migra o histórico).
- **Exclusão de paciente é anonimização, não DELETE, quando há vínculo clínico** — CFM exige retenção de prontuário por 20 anos; só o vínculo de identidade é apagado (nome, e-mail, CPF, contato viram irreversíveis), o registro clínico permanece.
- **Rate limiting** em `/auth/**`, `/export/**`, `/prontuarios/**`, `/documentos/**`, `/nps/**` (M11), `/waitlist/**` (M13) — 30 req/min por IP (Bucket4j), 429 ao estourar.
- **Consentimento LGPD registrado e versionado** — `terms_accepted_at` + `terms_version` no cadastro do paciente, não só um booleano.
- **MFA/TOTP implementado (27/08/2026), aguardando code review/homologação** — login em duas etapas pra ADMIN, PROFESSIONAL e PATIENT (RFC 6238, códigos de backup de uso único). Fundação de schema era da migration V10 (`users.mfa_enabled`/`totp_secret`); agora com fluxo completo: `TotpService` (TOTP escrito à mão, ±1 passo de tolerância a clock drift), `MfaService`/`MfaController` (`/auth/mfa/setup|enable|disable|verify`), `mfa_backup_codes` (migration V21). Peça central: login com `mfaEnabled=true` NUNCA emite o cookie de sessão `token` — só um `mfa_pending_token` curto (5min, claim `mfaPending`), que `SecurityFilter` não reconhece como sessão válida; a sessão real só nasce em `POST /auth/mfa/verify`. **Pendência de deploy:** requer provisionar `secrets/mfa_backup_pepper.txt` na VPS antes de subir (mesmo padrão de `cpf_hash_pepper`) — sem isso o container de produção não sobe (Docker secret ausente).

## Épicos
> Histórias de usuário completas e critérios de aceite BDD em `./docs/spec.md`.

1. **E1 · Autenticação & Sessão** *(Grande · risco alto)* — login, registro de paciente, JWT + refresh rotativo, logout com revogação, verificação de e-mail, recuperação de senha, rate limiting.
2. **E2 · Gestão de Perfil** *(Pequeno)* — paciente e profissional veem/editam o próprio perfil; admin cadastra/edita/exclui profissionais.
3. **E3 · Disponibilidade & Agenda do Profissional** *(Médio)* — grade mensal por data específica (não recorrente por dia-da-semana), slots de 1h, regras de edição (só mês atual/próximo).
4. **E4 · Agendamento de Consultas** *(Grande · risco alto)* — máquina de estados completa (ver abaixo), aprovação do profissional, dupla confirmação, cancelamento, reagendamento, falta. Inclui lista de espera para cancelamentos (US-4.8) — 🔍 Implementado (15/08/2026, versão robusta com confirmação ativa, repriced pra Grande), aguardando code review/homologação — e lembrete via WhatsApp (US-4.7) — 🔲 Backlog, aprovado 10/08/2026, ainda não desenvolvido (depende de decisão de provedor/orçamento com o cliente).
5. **E5 · Prontuário Eletrônico** *(Médio · risco alto pela sensibilidade do dado)* — criação de prontuário pelo profissional; é o único ponto do sistema que conclui a consulta.
6. **E6 · Documentos Clínicos** *(Médio)* — upload de documento (texto ou PDF em base64, validado por magic bytes), controle de visibilidade pro paciente.
7. **E7 · Penalidades por Falta/Cancelamento Tardio** *(Médio · risco médio)* — advertência na 1ª infração, bloqueio de 15 dias na 2ª, desbloqueio administrativo (reset completo).
8. **E8 · Exportação de Dados & Portabilidade LGPD** *(Pequeno)* — export CSV (admin/profissional) e JSON com metadados de consentimento (paciente, Art. 18 V LGPD).
9. **E9 · Painel Administrativo** *(Médio)* — dashboards por role (admin/profissional/paciente) com métricas e listagens operacionais.
10. **E10 · Segurança & Conformidade LGPD** *(Transversal · risco alto)* — criptografia de campo, hash de CPF, anonimização, auditoria, consentimento. Não é uma tela, é um conjunto de garantias que atravessa todos os épicos acima.
11. **E11 · Satisfação do Paciente (NPS)** *(Pequeno)* — 🔍 Implementado (15/08/2026), testes unitários verdes, aguardando code review/homologação. NPS pós-consulta automático, disparado no mesmo gatilho de `CONCLUIDA` (`ProntuarioService.create()`), via link público de uso único (`NpsResponse`), sem exigir login.

## Máquina de estados principal

**Consulta (Appointment):**
```
AGUARDANDO_CONFIRMACAO ──aprovar (profissional)──→ AGENDADA ──confirmar-profissional──→ CONFIRMADA_PROFISSIONAL ──confirmar-paciente──→ CONFIRMADA ──(prontuário criado)──→ CONCLUIDA
        │                                                                                                                                    │
        └──recusar (profissional)──→ CANCELADA                                                    de qualquer estado: cancelar → CANCELADA · marcarFalta → FALTA · reagendar → volta pra AGENDADA
```
- `CONCLUIDA` é setado **só** por `ProntuarioService.create()` — não existe em `AppointmentService`. Conceitualmente pertence ao épico E5 (Prontuário), não ao E4.
- `reagendar` sempre volta pro estado `AGENDADA`, reiniciando o ciclo de dupla confirmação, mesmo que já estivesse `CONFIRMADA`.
- `marcarFalta` não tem guarda de estado prévio e sempre aciona penalidade (E7), independente de janela de tempo.
- `cancelar` pode ser feito pelo paciente, profissional dono, ou qualquer ADMIN, de qualquer estado, com justificativa obrigatória. Penalidade só se aplica se `< 24h` da consulta **e** o estado já passou de `AGUARDANDO_CONFIRMACAO`.


## Diretivas de Gestão (Regra de Ouro do Trello + Jira)
> **ATENÇÃO:** Toda vez que você (Claude/IA) criar, modificar ou deletar qualquer especificação funcional ou técnica nos arquivos `CLAUDE.md`, `ROADMAP.md`, `docs/spec.md` ou `design/DESIGN.md`, você é **OBRIGADO** a executar **os dois scripts** — `./scripts/trello_sync.py` e `./scripts/jira_sync.py` — para espelhar essa exata alteração no Trello e no Jira correspondentes (criando cards/issues no Backlog, atualizando os Critérios de Aceite ou arquivando o que foi cancelado). Documentação, Trello e Jira são a mesma entidade. Board Jira deste projeto: `LUC` em `ondaenterprise.atlassian.net` (credenciais em `.env.jira`, fora do controle de versão).

## Convenções
- Erros padronizados via `GlobalExceptionHandler` (`@RestControllerAdvice`) — `ExceptionDTO(message, code)`.
- DTOs como Java Records; controller nunca retorna `@Entity`.
- `snake_case` no banco, `camelCase` no Java/TypeScript.
- Diretiva Primária na Fase 4: não alterar sintaxe de código existente.

> ✅ **API REST `/api/v1` — decidido não aplicar (27/08/2026).** Nenhuma rota usa prefixo `/api` nem `/v1` (rotas são `/consultas`, `/auth`, `/patients` etc., direto na raiz, misturando português e inglês). Avaliado e descartado: sistema interno de uma única clínica, sem consumidor externo de API que exija versionamento REST — o custo da migração (contrato quebrado em backend + frontend + ~45 suítes de teste Cypress/JUnit) não se paga sem um motivo funcional real. Revisitar só se surgir um consumidor externo de verdade.

## Memória Técnica (Bugs e Decisões)
Vault Obsidian em [`./memoria-tecnica/`](./memoria-tecnica/_index.md), dentro do próprio repo — bugs
cabeludos resolvidos (causa raiz, não só sintoma) e decisões técnicas tomadas fora desta spec.

- **Antes de investigar um bug**, consultar `memoria-tecnica/bugs/` — pode já ter causa raiz documentada.
- **Antes de tomar decisão de arquitetura**, consultar `memoria-tecnica/decisoes/` — pode já existir uma decisão ativa sobre o assunto.
- **Ao resolver um bug não-trivial ou tomar uma decisão fora da spec**, registrar nota nova em `memoria-tecnica/` (templates em `memoria-tecnica/templates/`), linkando às notas relacionadas com a notação `[[nome-da-nota]]`.

## Ponteiros
- Histórias completas + BDD: `./docs/spec.md`
- Blueprint técnico: `./ROADMAP.md`
- Identidade visual: `./design/tokens.css` + `./design/DESIGN.md`
- Comandos de dev, ambiente local, notas de teste: `./docs/DEV.md`
- Memória técnica: `./memoria-tecnica/`
