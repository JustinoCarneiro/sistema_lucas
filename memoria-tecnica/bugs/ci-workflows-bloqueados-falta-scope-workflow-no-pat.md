---
tipo: bug
data: 
severidade: Média
status: Resolvido (29/07/2026, na correção de metodologia)
---

# CI desligado há meses: workflows movidos pra fora de `.github/workflows/` por bloqueio do GitHub

## Sintoma
`.github/workflows/` estava vazio; os arquivos reais (`ci-backend.yml`, `ci-frontend.yml`) estavam "arquivados" numa pasta `.github-workflows-backup/` fora do diretório ativo, gitignorada, com o comentário "Backup dos workflows bloqueados pelo GitHub". CI não rodava havia muito tempo — nenhum registro em `.git log` de quando isso aconteceu (a pasta nunca foi commitada, nem os workflows originais).

## Causa raiz
Explicação mais provável (não confirmada com certeza absoluta, mas é o padrão clássico): o GitHub **rejeita pushes que criam/alteram arquivos em `.github/workflows/*.yml`** quando o Personal Access Token usado não tem o escopo `workflow` habilitado — retorna erro explícito de permissão. A reação foi mover os arquivos pra fora do caminho vigiado pra desbloquear outros pushes, sem nunca voltar pra corrigir o escopo do token e restaurar os workflows no lugar certo.

## Solução
Arquivos restaurados em `.github/workflows/` (também corrigido `java-version` de `'21'` pra `'17'` no `ci-backend.yml`, alinhando à versão real do `pom.xml` — ver `CLAUDE.md`). Pasta de backup removida.

**Atenção:** o próximo `git push` só vai funcionar se o token/credencial usado tiver o escopo `workflow` (PAT clássico) ou permissão de "Workflows: write" (fine-grained token). Se o push desses arquivos for rejeitado de novo com erro de permissão, é exatamente essa causa raiz confirmada — gerar um token com o escopo correto, não repetir o padrão de mover os arquivos pra fora.
