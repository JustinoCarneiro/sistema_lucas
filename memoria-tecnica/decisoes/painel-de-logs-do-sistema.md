---
tipo: decisao
data: 2026-08-28
status: Ativa
---

# Painel de logs do sistema (`/panel/logs`, ADMIN/TECNICO)

## Contexto
O SMTP do sistema ficou fora do ar por tempo indeterminado sem nenhum log visível — descoberto
só por acaso, num cadastro de teste durante homologação (ver
[[smtp-gmail-autenticacao-falhando]]). Root cause de por que ninguém percebeu antes: o
`catch (MailException e)` do `EmailService` (que é o único motivo de essa falha aparecer em log
nenhum) foi adicionado só no mesmo dia em que o problema foi descoberto — antes disso, a exceção
de `mailSender.send()` era unchecked, não pega pelo catch mais estreito que já existia, e
desaparecia sem rastro (método `@Async`).

## Decisão
Captura automática de todo WARN/ERROR logado em qualquer parte do backend via SLF4J (sem
instrumentação manual ponto-a-ponto), retido 30 dias, visível só pra ADMIN/TECNICO.

Arquitetura: `InMemoryLogAppender` (Logback puro, roda antes do Spring subir — por isso não grava
direto no banco) enfileira em `ConcurrentLinkedQueue` estática (cap 5000, descarta o mais antigo).
`SystemLogPersistenceService` drena e persiste em lote a cada 10s (`@Scheduled`) e expurga
registros com mais de 30 dias diariamente. `GET /system-logs`
(`@PreAuthorize("hasAnyRole('ADMIN','TECNICO')")`, paginado, filtro por level). Frontend em
`/panel/logs`.

6 `System.err.println` que não passavam pelo SLF4J foram convertidos pra `logger.error`/`warn`
(`EmailService`, `LembreteScheduler`, `DataInitializer`) — sem isso a "captura de tudo" teria o
mesmo ponto cego que causou o problema original.

## Consequências
Mensagem de log pode conter PII incidental (ex.: e-mail de paciente numa mensagem de erro) —
aceito como exposição equivalente à que `docker logs` já dava pra quem tem acesso SSH à VPS, não
é superfície nova.

Provou o próprio valor no dia seguinte ao ser implementado: foi essencial pra diagnosticar
[[waitlist-oferta-nao-persistia-after-commit]] (mostrou que a ausência de qualquer log era, ela
mesma, um sintoma — forçou instrumentação diagnóstica temporária pra achar a causa raiz real).

## Ligado a
- [[smtp-gmail-autenticacao-falhando]]
- [[waitlist-oferta-nao-persistia-after-commit]]
