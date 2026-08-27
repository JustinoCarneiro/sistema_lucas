---
tipo: bug
data: 2026-08-27
severidade: Alta
status: Resolvido
---

# Deploy de produção entrou em crash loop — migration V20 batia em duplicidade real pré-existente

## Sintoma
Primeiro deploy de produção em 6 semanas (`push-and-deploy.sh`) subiu a imagem nova, mas
`lucas-api` entrou em crash loop (`restart: on-failure` reiniciando a cada poucos segundos,
sempre falhando no mesmo ponto). Site e API ficaram fora do ar até a correção.

## Causa raiz
Migration V20 (`ALTER TABLE prontuarios ADD CONSTRAINT uk_prontuario_appointment UNIQUE
(appointment_id)`, deste mesmo commit) falhou com `23505 duplicate key` — já existia duplicidade
real em produção, de antes da correção existir: `appointment_id=9` tinha 2 prontuários (ids 2 e
3, criados com 298ms de diferença em 09/05/2026), um duplo-submit clássico no "Salvar e finalizar
atendimento" que aconteceu meses antes de qualquer proteção (nem a checagem de idempotência em
`ProntuarioService.create()`, nem a V20) existir.

**Efeito colateral que atrapalhou a primeira tentativa de correção:** `push-and-deploy.sh` faz
`rsync -avz` **sem `--delete`** — um arquivo removido/renomeado localmente não some do servidor
no próximo deploy, só fica um arquivo novo ao lado do antigo. Renomear
`V20__....sql` → `V20__....sql.disabled` localmente e redeployar não teve efeito nenhum na
primeira tentativa, porque o `V20__....sql` original continuava intacto no servidor — Flyway
achou os dois arquivos e continuou rodando o antigo. Só foi resolvido rodando
`rsync --delete` manualmente contra a pasta de migrations, ou apagando o arquivo direto no
servidor.

## Solução
1. Restaurar o serviço sem tocar em dado: `V20` desativada permanentemente (`.sql.disabled`,
   Flyway ignora), sem apagar/alterar os dois prontuários duplicados — ver
   [[uk-prontuario-appointment-nao-aplicada-dado-legado]] pra decisão completa e o porquê disso
   ser seguro (a proteção real já está em `ProntuarioService.create()`, não na constraint).
2. Confirmado via smoke test real (curl no domínio de produção, não só status do container) —
   hábito já documentado em `saw-hub-coolify-deploy-verification` (memória do projeto SAW HUB,
   mesma VPS compartilhada) valeu de novo aqui.

**Regra geral:** `push-and-deploy.sh` **não remove arquivos deletados/renomeados no servidor**.
Qualquer deploy que remove ou renomeia um arquivo (não só adiciona/edita) precisa ser conferido
manualmente no servidor, ou o script precisa ganhar `--delete` no rsync (não corrigido ainda,
risco de `--delete` também remover coisas no servidor que não deveriam sumir — `.env`/`secrets/`
já são excluídos do rsync, então não seriam afetados, mas vale revisar as exclusões antes de
adicionar `--delete` de verdade).

## Ligado a
- [[uk-prontuario-appointment-nao-aplicada-dado-legado]] — a decisão de não tocar nos dados.
- [[h2-nao-suporta-indice-unico-parcial-em-teste]] — mesma migration V20, achado anterior no
  mesmo dia, em contexto de teste.
