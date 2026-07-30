---
tipo: bug
data: 2026-06-03
severidade: Média
status: Resolvido
---

# Teste "time-bomb": `naoNotificaQuandoJaTemAgenda` falhava ~29 dias por mês

## Sintoma
Teste `naoNotificaQuandoJaTemAgenda` (`LembreteSchedulerTest`) ficava vermelho na quase totalidade dos dias do mês, verde só ocasionalmente.

## Causa raiz
O teste dependia de uma condição amarrada ao dia do mês real (`diasParaFim == 10`), calculada a partir de `LocalDate.now()` em vez de uma data fixada/injetada — um "time-bomb test" clássico, só passava quando a data de execução coincidia com a condição esperada.

## Solução
Removido o stub desnecessário que criava essa dependência — o mock já retornava lista vazia por padrão, o que já satisfazia o cenário sem precisar de uma condição de data.

**Regra geral:** todo teste que envolve `LocalDate.now()`/`LocalDateTime.now()` precisa injetar ou fixar a data (`Clock` fixo, ou parâmetro explícito) — nunca deixar a asserção depender do dia real em que o teste roda. Um teste que só falha em certos dias do mês é um sinal claro desse antipadrão.

## Ligado a
- [[unicidade-cadastro-inconsistente-entre-entry-points]] — achado na mesma leva de auditoria (03/06/2026).
