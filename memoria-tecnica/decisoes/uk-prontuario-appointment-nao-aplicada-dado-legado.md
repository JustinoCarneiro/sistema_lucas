---
tipo: decisao
data: 2026-08-27
status: Resolvida (mesmo dia) — ver atualização no final
---

# Constraint única `uk_prontuario_appointment` (V20/V22) — histórico completo do incidente

> ✅ **Resolução final (27/08/2026):** o usuário verificou o conteúdo real pelo próprio sistema
> (login, histórico de prontuário do paciente na UI — texto descriptografado normalmente) e
> confirmou **conteúdo idêntico** nos pares duplicados. Com a incerteza original resolvida, a
> decisão abaixo foi revertida: os registros duplicados foram apagados manualmente em produção
> (mantendo sempre o mais antigo de cada par como o válido) e a constraint foi reaplicada como
> **V22** (mesmo SQL da V20, renumerada pra não reescrever um número de versão que já tinha
> falhado uma vez em prod). Migration V22 deployada com sucesso, tabela sem nenhum
> `appointment_id` duplicado. O texto abaixo (Contexto/Decisão originais) fica só como registro
> histórico de como e por que a primeira decisão foi tomada.
>
> **Lição importante que não estava no relato original:** a primeira tentativa de correção só
> reagiu ao *primeiro* erro que o Flyway reportou (`appointment_id=9`) — só depois de já ter
> reativado a migration uma vez e ela falhar de novo (agora em `appointment_id=50`) é que rodou
> um `GROUP BY appointment_id HAVING COUNT(*) > 1` pra levantar **todos** os pares de uma vez.
> Resultado: 3 pares no total (`appointment_id` 9, 16 e 50), não 1. **Regra geral pra próxima
> vez:** ao investigar uma constraint única que falha por dado duplicado, sempre levantar
> *todos* os grupos duplicados antes de decidir/corrigir, nunca reagir só ao primeiro erro do
> Flyway — ele para na primeira violação, não lista as demais.

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

## Consequências (do estado final, pós-resolução)
- `uk_prontuario_appointment` está ativa em produção (V22) — nenhum `appointment_id` pode mais
  ter mais de um prontuário, a nível de banco, não só de aplicação.
- Os 3 pares duplicados (`appointment_id` 9, 16, 50) tiveram o registro mais recente apagado —
  `prontuarios.id` 3, 9 e 17 não existem mais. Isso foi uma exceção deliberada e única à regra
  geral do projeto de nunca fazer `DELETE` físico de prontuário — só aconteceu depois de
  confirmação visual do usuário, pelo próprio sistema, de que o conteúdo era idêntico (duplo-
  submit, não duas anotações clínicas distintas).
- Efeito colateral descoberto neste incidente: `push-and-deploy.sh` usa `rsync` sem `--delete`,
  então um arquivo removido/renomeado localmente **não** some do servidor sozinho no próximo
  deploy — ficou um arquivo fantasma (`V20__....sql`, sem o `.disabled`) que mascarou a primeira
  tentativa de correção. Ainda não corrigido no script (fora do escopo deste incidente); qualquer
  remoção de arquivo em deploy futuro precisa ser conferida manualmente no servidor, ou usar
  `rsync --delete` diretamente contra a pasta específica (foi o que resolveu na 2ª rodada deste
  mesmo incidente).
- Se uma constraint única nova algum dia falhar de novo por dado legado: **levantar todos os
  grupos duplicados de uma vez** (`GROUP BY ... HAVING COUNT(*) > 1`) antes de investigar/corrigir
  qualquer um — não reagir só ao primeiro erro do Flyway.

## Ligado a
- [[h2-nao-suporta-indice-unico-parcial-em-teste]] — mesma migration V20 apareceu antes, num
  contexto de teste (achado na revisão minuciosa do mesmo dia).
