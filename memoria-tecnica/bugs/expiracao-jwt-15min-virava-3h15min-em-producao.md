---
tipo: bug
data: 2026-08-27
severidade: Alta
status: Resolvido
---

# `TokenService` gerava access token JWT com validade de ~3h15min em vez de 15min

## Sintoma
Nenhum sintoma visível em dev (rodando com timezone `-03:00` na máquina/container local) — só
apareceria em produção, onde a JVM roda em UTC, e mesmo lá só seria percebido indiretamente
(sessão "grudando" por mais tempo que o esperado, janela de exposição maior que a documentada
em caso de token vazado).

## Causa raiz
`genExpirationDate()` calculava a expiração assim:
```java
LocalDateTime.now().plusMinutes(15).toInstant(ZoneOffset.of("-03:00"))
```
`LocalDateTime.now()` já retorna a hora **local da JVM** (sem timezone embutido). Em produção a
JVM roda em UTC — então esse `LocalDateTime` já está em UTC. Convertê-lo pra `Instant` fixando
explicitamente o offset `-03:00` soma 3h a mais na conversão (trata um horário UTC como se fosse
horário de Brasília), inflando os 15 minutos de validade do access token pra ~3h15min sem
nenhum erro ou log — o token simplesmente ficava válido por muito mais tempo que o pretendido.

## Solução
Trocado para `Instant.now().plusSeconds(15 * 60)` — sem conversão de timezone nenhuma, já que
`Instant` é absoluto (UTC) por definição. Elimina a classe inteira de bug: qualquer combinação
`LocalDateTime.now()` + `.toInstant(offsetFixo)` é suspeita quando o offset da JVM em produção
não é garantidamente o mesmo do offset hardcoded no código.

**Regra geral:** pra gerar timestamps absolutos (expiração de token, agendamento de job), usar
`Instant.now()` ou `ZonedDateTime.now(ZoneOffset.UTC)` — nunca `LocalDateTime.now()` combinado
com um offset fixo no código. `LocalDateTime` não carrega timezone; assumir qual é o timezone da
JVM que vai rodar o código é frágil e o erro fica invisível até alguém comparar timestamps.

## Ligado a
- [[h2-nao-suporta-indice-unico-parcial-em-teste]] — achado na mesma revisão minuciosa de todo o
  sistema (E1–E10, não só o M11/M13 mais recente), em 27/08/2026.
