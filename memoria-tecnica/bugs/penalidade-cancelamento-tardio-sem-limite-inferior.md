---
tipo: bug
data: 2026-09-01
severidade: Alta
status: Resolvido
---

# Penalidade de cancelamento tardio disparava pra consulta no passado

## Sintoma
"Uma galera não consegue agendar." Pacientes recebiam o toast **"Erro Detectado — Você está
temporariamente bloqueado para novos agendamentos até dd/MM/yyyy HH:mm devido a um
cancelamento/reagendamento tardio"** sem ter feito nada de errado recentemente. Um caso
concreto reportado: paciente cancelou uma consulta de **15/07** e foi bloqueado até **15/09**.
Recepção interpretava como bug de sistema (a mensagem começa com "Erro Detectado").

## Causa raiz
`AppointmentService.aplicarPenalidadeSeNecessario()` checava só o **limite superior** da janela
de 24h:

```java
if (LocalDateTime.now().isAfter(consulta.getDateTime().minusHours(24))) { ... }
```

Isso é verdadeiro pra consulta a menos de 24h (correto) **e pra toda consulta no passado**
(`now` está sempre depois de `dataConsulta − 24h`). Como consulta só vira `CONCLUIDA` via
`ProntuarioService.create()`, existe muita consulta antiga parada em `AGENDADA`/`CONFIRMADA` que
nunca fechou. Qualquer paciente que cancelasse ou reagendasse uma dessas levava infração de
"cancelamento tardio" — e, se já tinha a 1ª advertência, bloqueio de 15 dias contado a partir de
`now` (por isso o "até" tinha minuto quebrado: era `now.plusDays(15)` do momento da ação).

Agravantes na mesma investigação:
- `cancelar()` não tinha guarda de estado terminal — dava pra "cancelar" consulta já
  `CANCELADA`/`CONCLUIDA`/`FALTA`. Cada re-chamada (duplo-clique/retry) rodava
  `aplicarPenalidadeSeNecessario` de novo e reincrementava `infractionCount` — dois cliques
  bastavam pra advertência + bloqueio numa tacada.
- `reagendar()` idem: reagendar consulta encerrada a ressuscitava pra `AGENDADA` e empilhava
  infração.
- `marcarFalta()` não era idempotente: remarcar uma `FALTA` já existente reincrementava a infração.

## Solução
`aplicarPenalidadeSeNecessario` agora exige a janela real — consulta **ainda no futuro** E a
menos de 24h:

```java
boolean consultaNoFuturo = agora.isBefore(consulta.getDateTime());
boolean faltamMenosDe24h = agora.isAfter(consulta.getDateTime().minusHours(24));
if (consultaNoFuturo && faltamMenosDe24h) { processarInfracaoPaciente(...); }
```

Guarda de estado terminal (`ehEstadoTerminal` = `CANCELADA`/`CONCLUIDA`/`FALTA`) adicionada em
`cancelar()`, `reagendar()` e `marcarFalta()` — consulta encerrada rejeita a operação em vez de
reprocessar penalidade. `marcarFalta` continua **sem** guarda de janela de tempo (regra E7).

Regressão coberta em `AppointmentServiceTest` (5 casos novos): cancelar/reagendar consulta no
passado não penaliza; cancelar/reagendar/marcar-falta em consulta encerrada lança erro sem mexer
em `infractionCount`. Suíte de backend inteira verde (213 testes).

Cosmético (frontend): adicionado tipo de toast `warning` ("Atenção", âmbar) e a mensagem de
bloqueio passou a sair por ele em `my-appointments` (`notifyScheduleError` — match em
"temporariamente bloqueado"), em vez do vermelho "Erro Detectado" que a recepção lia como bug.

## Estrago em produção (apurado no audit_logs em 01/09/2026)

Só **4 `BLOQUEIO_FALTAS` na vida do sistema**, todos no cluster 26–31/08/2026, nos pacientes
**30 (Ana Clara), 57 (Esther) e 109 (Adrielly)**. Causa: a profissional `aguarana@gmail.com`
(agenda "Guaraná") cancelou em 26/08 13:50, **em 50 segundos**, três consultas encalhadas do dia
19/08 (ids 105, 109, 112) — cada cancelamento de consulta 7 dias no passado disparou infração de
"cancelamento tardio" e, como os 3 já tinham `received_first_warning=true`, bloqueio de 15 dias.
Adrielly levou um 2º bloqueio em 31/08 por FALTA real (consulta 113) — legítima na regra E7, mas
sobre contagem já poluída.

Os `received_first_warning=true` vieram de **outro lote do mesmo bug**: 18/07 17:48–17:49,
pacientes **30, 109 e 22** advertidos em 32 segundos (mesma assinatura de cancelamento em lote).
Paciente 22 ficou com advertência falsa mas nunca foi bloqueado.

Remediação 01/09/2026: reset via `PATCH /patients/{id}/desbloquear` (ou UPDATE direto —
`blocked_until=NULL, infraction_count=0, received_first_warning=false`) nos ids **30, 57, 109**
(bloqueados) e **22** (gatilho armado). As demais advertências (32, 68, 92, 94, 97, 104, 106,
113, 114) estão espalhadas no tempo, plausivelmente legítimas, nenhuma virou bloqueio.

Pendências fora deste fix:
- `blocked_until` não tem migration Flyway — só existe em prod por `ddl-auto=update` histórico.
  Formalizar numa `V23`.
- Toda vez que um profissional/admin faz faxina de consultas antigas cancelando-as, ele
  penaliza os pacientes. O fix corrige a regra; considerar também uma tela/ação de
  "arquivar consulta encalhada" que não passe por `cancelar()`.

## Ligado a
- [[email-template-format-exception-bloqueou-agendamento]]
- [[waitlist-oferta-nao-persistia-after-commit]]
