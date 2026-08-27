---
tipo: decisao
data: 2026-08-27
status: Resolvida (mesmo dia) — ver atualização no final
---

# Constraint única `uk_prontuario_appointment` (V20) não aplicada em produção — dado legado

> ✅ **Atualização, mesmo dia (27/08/2026):** o usuário pediu pra verificar o conteúdo real das
> duas entradas pelo próprio sistema (login, abrir o histórico de prontuário do paciente
> [Luiz Guimarães Neto](#), `patient_id=26`) — texto **confirmado idêntico**. Com a incerteza
> original resolvida, o usuário decidiu reverter a decisão abaixo: `prontuarios.id=3` foi
> **apagado manualmente** em produção (mantendo `id=2` como o registro válido), e a constraint
> foi reaplicada como **V22** (mesmo SQL da V20, renumerada pra não reescrever um número de
> versão que já tinha falhado uma vez em prod). Migration V22 já deployada com sucesso. O texto
> abaixo fica como registro de como e por que a decisão original foi tomada — a decisão em si
> não vale mais.

## Contexto
No deploy de produção de 27/08/2026 (primeiro em 6 semanas, trazendo todo o trabalho de M11/M13
+ revisão minuciosa + MFA acumulado), a migration V20 (`ALTER TABLE prontuarios ADD CONSTRAINT
uk_prontuario_appointment UNIQUE (appointment_id)`) falhou e derrubou o backend em crash loop
(`restart: on-failure` reiniciando o container repetidamente, todos falhando na mesma migration).

Causa: já existe duplicidade real em produção, de **antes** desta correção existir —
`appointment_id=9` tem 2 prontuários (`prontuarios.id` 2 e 3), criados com **298ms de diferença**
em 09/05/2026 (13:37:14.044782 e 13:37:14.342621) — assinatura clássica de duplo-clique/retry no
"Salvar e finalizar atendimento", exatamente o bug que a V20 (e a checagem de idempotência em
`ProntuarioService.create()`, do mesmo commit) foram desenhadas pra prevenir dali pra frente. Os
dois registros têm mesmo `patient_id`/`professional_id` e mesmo tamanho de conteúdo cifrado (588
bytes) — indício forte (não prova, o conteúdo é criptografado) de que são a mesma submissão
duplicada, não duas consultas clínicas distintas.

## Decisão
**Não apagar nem alterar nenhum dos dois registros.** São dados de saúde de paciente — LGPD/CFM
exigem retenção de prontuário por 20 anos, e decidir sob pressão de um incidente em produção não
é hora de tomar uma decisão irreversível sobre dado clínico. O usuário foi explícito: perda de
dado sensível em produção é inaceitável, mesmo que o registro pareça claramente duplicado.

A migration V20 foi renomeada pra `.sql.disabled` (Flyway ignora, só processa `V*__*.sql`) e
**fica desativada permanentemente** — não é uma pendência a resolver depois, é a decisão final.

**Por que isso é seguro:** a proteção real contra esse bug não é a constraint de banco, é a
checagem em `ProntuarioService.create()` (`prontuarioRepository.existsByAppointmentId(appointmentId)`
antes de salvar, lança "Esta consulta já tem um prontuário registrado." se já existir) — essa
checagem já está em produção desde este mesmo deploy e já impede a criação de um novo duplicado,
independente da constraint de banco existir ou não. A V20 seria só uma segunda camada de defesa
em profundidade (fecha a corrida check-then-act a nível de banco), não a proteção primária.

## Consequências
- Os dois prontuários (`id=2` e `id=3`, `appointment_id=9`) continuam existindo pra sempre, sem
  constraint impedindo esse tipo de duplicidade a nível de banco especificamente para eles (ou
  qualquer duplicidade futura, na hipótese — não confirmada — de algum outro caminho de código
  algum dia bypassar a checagem em `ProntuarioService`).
- **Nunca renomear `V20__....sql.disabled` de volta pra `.sql`** sem antes decidir explicitamente
  com o usuário o que fazer com esses dois registros — reativar a migration sem resolver isso
  primeiro reproduz o mesmo crash loop de 27/08/2026.
- Efeito colateral descoberto neste incidente: `push-and-deploy.sh` usa `rsync` sem `--delete`,
  então um arquivo removido/renomeado localmente **não** some do servidor sozinho no próximo
  deploy — ficou um arquivo fantasma (`V20__....sql`, sem o `.disabled`) que mascarou a primeira
  tentativa de correção. Não corrigido no script ainda (fora do escopo deste incidente); qualquer
  remoção de arquivo em deploy futuro precisa ser conferida manualmente no servidor até isso ser
  corrigido.

## Ligado a
- [[h2-nao-suporta-indice-unico-parcial-em-teste]] — mesma migration V20 apareceu antes, num
  contexto de teste (achado na revisão minuciosa do mesmo dia).
