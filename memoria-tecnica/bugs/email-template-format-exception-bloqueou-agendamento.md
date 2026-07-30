---
tipo: bug
data: 2026-05-27
severidade: Crítica
status: Resolvido
---

# `MissingFormatArgumentException` em template de e-mail derrubou todo o fluxo de agendamento

## Sintoma
Todos os fluxos que enviam e-mail transacional (agendar, pendente, solicitação ao profissional, aceito, recusado, confirmado, cancelado) quebravam em produção.

## Causa raiz
`EmailTemplateService.buildTemplate` fechava um text block Java prematuramente e chamava `.formatted(frontendUrl)` com **1 argumento** sobre um bloco que continha **7 placeholders `%s`** — `MissingFormatArgumentException` em runtime, não detectável em compilação (text blocks + `.formatted()` não têm checagem estática de aridade).

## Solução
Text block unificado, `frontendUrl` passado como o 7º argumento correto do `.formatted()` final. Adicionados 7 testes de regressão em `EmailTemplateServiceTest`, um por fluxo público que chega em `buildTemplate`.

**Regra geral:** `String.formatted()`/`String.format()` sobre um template com múltiplos placeholders `%s` não tem checagem de aridade em tempo de compilação — qualquer alteração no número de placeholders de um template precisa de teste que efetivamente renderize a string, não só verificar que o método não lança exceção com um caso feliz.
