---
tipo: indice
---

# Memória Técnica — Sistema Lucas

Base de conhecimento viva do projeto: bugs cabeludos já resolvidos (com causa raiz) e decisões técnicas
tomadas fora da spec original do [`CLAUDE.md`](../CLAUDE.md). Não documenta conceitos genéricos — só o
que é específico deste projeto e não seria óbvio olhando só o código.

Padrão da metodologia Onda-Dev — ver seção 11 de
[`Metodologia_de_Desenvolvimento_-_Onda.md`](../docs/Metodologia_de_Desenvolvimento_-_Onda.md).

Este projeto não tinha nenhum artefato da metodologia Onda antes de 29/07/2026 — todo o conteúdo
aqui (e em `CLAUDE.md`/`docs/spec.md`/`ROADMAP.md`/`design/`) foi reconstruído retroativamente a
partir do código real, não escrito durante o desenvolvimento original.

## Como usar
- **Antes de investigar um bug**, procurar em `bugs/` se algo parecido já foi resolvido.
- **Antes de tomar uma decisão de arquitetura**, procurar em `decisoes/`.
- **Ao resolver um bug não-trivial ou tomar uma decisão fora da spec**, criar nota nova usando
  `templates/bug.md` ou `templates/decisao.md`, linkando com a notação `[[nome-da-nota]]`.

## Bugs
- [[backup-diario-quebrado-working-directory-cron]] — falha silenciosa de ~2 meses
- [[email-template-format-exception-bloqueou-agendamento]] — derrubou todo o fluxo de agendamento em produção
- [[unicidade-cadastro-inconsistente-entre-entry-points]]
- [[teste-time-bomb-lembrete-scheduler]]
- [[ci-workflows-bloqueados-falta-scope-workflow-no-pat]] — resolvido nesta mesma correção de metodologia
- [[expiracao-jwt-15min-virava-3h15min-em-producao]] — timezone da JVM em prod (UTC) vs offset hardcoded no código
- [[h2-nao-suporta-indice-unico-parcial-em-teste]] — trava real (índice parcial) nunca era testada
- [[jira-team-managed-endpoints-bloqueados]] — campo→layout é gap real de API; delete de issue era falta de papel atribuído, não limitação de plataforma
- [[deploy-crash-loop-v20-dado-legado-duplicado]] — primeiro deploy em 6 semanas caiu em crash loop; rsync sem --delete mascarou a 1ª tentativa de correção
- [[ci-backend-hibernate-postinitcallback-so-no-runner]] — CI do backend falha só no GitHub Actions (Hibernate SQM), não reproduz local; aberto, não investigado a fundo

## Decisões
- [[mfa-totp-fundacao-de-schema-sem-fluxo-ativo]] — superada 27/08/2026, MFA implementado
- [[backup-criptografado-offsite-drive]]
- [[uk-prontuario-appointment-nao-aplicada-dado-legado]] — nunca apagar prontuário duplicado pra "resolver" a migration; proteção real já está no código
