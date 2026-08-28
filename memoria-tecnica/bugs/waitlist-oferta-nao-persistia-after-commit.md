---
tipo: bug
data: 2026-08-28
severidade: alta
status: Resolvido
---

# Lista de espera: a oferta de vaga nunca persistia (silenciosamente) após o cancelamento

## Sintoma
Durante a homologação ao vivo do E4/US-4.8 (lista de espera de cancelamento), reproduzido via
curl direto contra produção: paciente A agenda um horário, paciente B entra na lista de espera
pro mesmo horário, paciente A cancela. O e-mail de "vaga disponível" pro paciente B (e o de
"nova solicitação" pro profissional) disparavam normalmente — mas a `Appointment` reservada e a
atualização da `WaitlistEntry` (status → `OFERECIDA`, token, prazo) **nunca apareciam no banco**.
A entrada ficava presa em `AGUARDANDO` pra sempre, sem nenhuma exceção logada em lugar nenhum
(nem console, nem no `system_logs` novo — ver [[painel-de-logs-do-sistema]]). Reproduzido de
forma limpa e determinística 2x seguidas contra produção, então não era uma falha intermitente.

## Causa raiz
`WaitlistService.ofertarProximoDaFila()` é `@Transactional` (propagação padrão `REQUIRED`) e é
chamado via `self.` de dentro de `aoCancelarConsulta()`, um
`@TransactionalEventListener(phase = AFTER_COMMIT)` — ou seja, roda **na mesma thread da
requisição HTTP** que processou o `cancelar()` original, só que depois da transação desse
cancelamento já ter comitado.

Com `spring.jpa.open-in-view=true` (o padrão do Spring Boot, ativo neste projeto — o próprio WARN
disso já é capturado pelo painel de logs), o `OpenEntityManagerInViewInterceptor` mantém um
`EntityManager` vinculado à thread da requisição inteira, não só à transação do `cancelar()`.
Quando `ofertarProximoDaFila()` abre com `REQUIRED` (participa de transação existente ao invés de
criar uma nova), ele acaba "participando" desse `EntityManager` do OSIV — que Spring reporta como
tendo transação ativa (`TransactionSynchronizationManager.isActualTransactionActive()` = `true`),
mas cujo estado real, por já ter passado por um commit anterior nessa mesma requisição, não
suporta mais um `flush()` de verdade: falha com `org.springframework.transaction.CannotCreateTransactionException`/
`no transaction is in progress` — só que **isso só acontece no flush implícito do commit**, ou
seja, depois que o método já tinha "retornado normalmente" pro `try` de `aoCancelarConsulta`, tarde
demais pro `catch (Exception e)` capturar qualquer coisa útil (o log de erro real fica associado
ao commit, não à chamada em si).

Diagnóstico confirmado ao vivo em produção adicionando logs temporários (não deixados no código
final) que capturaram, na ordem: `transacao ativa antes do save? true` →
`appointment.getId()=null` mesmo após `save()` → e só ao forçar um `appointmentRepository.flush()`
explícito, a exceção real apareceu: `"no transaction is in progress"`.

Achado colateral que mascarava o sintoma: como `EmailService.enviar()` é `@Async`
(fire-and-forget, thread separada), os e-mails de notificação disparavam **antes** de qualquer
sinal do problema — dando a falsa impressão de que o fluxo tinha funcionado.

## Solução
`@Transactional(propagation = Propagation.REQUIRES_NEW)` em `ofertarProximoDaFila()` — força o
Spring a suspender o recurso (o `EntityManager` do OSIV) preso à thread e abrir uma transação
genuinamente nova e independente, em vez de tentar reaproveitar o que já estava vinculado. Mesma
técnica que `AppointmentService.cancelarPorExpiracaoDeOferta()` já usava, só que por um motivo
diferente ali (isolar falhas de item a item num loop de scheduler, não esse problema de OSIV).

Corrigido também, como efeito colateral positivo do mesmo trecho: o código reatribuía
`entry.setAppointment(appointment)` usando a variável **pré-save**, em vez do objeto retornado por
`appointmentRepository.save(appointment)` — inofensivo com `REQUIRES_NEW` funcionando
corretamente, mas foi corrigido pra usar o retorno de `save()`, que é a prática correta.

## Ligado a
- [[painel-de-logs-do-sistema]] — foi literalmente a investigação da homologação do E4/US-4.8 que
  motivou construir o painel de logs no dia anterior; a ausência de qualquer rastro de erro nesse
  bug (nem WARN, nem ERROR, em lugar nenhum, nem console nem `/panel/logs`) foi o que forçou
  adicionar logging diagnóstico temporário pra achar a causa raiz.
- `memoria-tecnica/bugs/smtp-gmail-autenticacao-falhando.md` — SMTP quebrado é o que expôs esse
  bug: se o e-mail estivesse funcionando, o comportamento observável (e-mail "chegou" mas vaga
  nunca foi reservada de fato) teria sido ainda mais confuso de diagnosticar por quem só olhasse
  a caixa de entrada.
