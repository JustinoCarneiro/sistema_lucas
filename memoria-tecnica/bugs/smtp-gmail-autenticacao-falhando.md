---
tipo: bug
data: 2026-08-27
severidade: Alta
status: Aberto — aguardando ação do cliente (nova senha de app Gmail)
---

# SMTP do Gmail com falha de autenticação — nenhum e-mail do sistema sai

## Sintoma
Descoberto durante homologação de MFA/lista de espera/NPS: um cadastro de teste não recebeu
e-mail de verificação. `docker logs lucas-api` mostrou `MailException: Authentication failed` ao
tentar enviar via SMTP do Gmail (`institutolucas9@gmail.com`).

## Causa raiz (parcial)
A senha de app salva em `secrets/mail_password.txt` estava com espaços (formato de exibição do
Google, "xxxx xxxx xxxx xxxx" — 19 caracteres em vez de 16) — corrigido, container recriado,
confirmado que o container passou a ter o valor de 16 caracteres certo. **Mesmo assim, a
autenticação continua falhando.** Ou seja, a formatação era um problema real (deveria ser
corrigida de qualquer forma), mas não é a causa raiz completa — a credencial em si provavelmente
está inválida (revogada, expirada, ou a verificação em duas etapas da conta Google mudou desde
que a senha foi gerada).

**Achado colateral importante:** esse erro nunca tinha aparecido em log nenhum antes de hoje,
porque `EmailService` só passou a capturar `MailException` numa correção da revisão minuciosa
desta mesma sessão — antes disso, a falha desaparecia silenciosamente (método `@Async`, exceção
nunca chegava em lugar visível). Não há como saber com certeza há quanto tempo o e-mail está
quebrado.

## Por que não foi corrigido
Só quem tem acesso à conta Google `institutolucas9@gmail.com` consegue gerar uma senha de app
nova (myaccount.google.com/apppasswords) — é a conta do cliente, o usuário (Marcos) não tem esse
acesso. Registrado como pendência — ver memória pessoal
`smtp-gmail-auth-falhando-precisa-nova-senha-app` (fora deste repo).

## Solução (quando a senha nova existir)
1. Atualizar `secrets/mail_password.txt` na VPS com a senha nova, **sem espaços**.
2. `docker compose up -d --force-recreate backend` — um `docker restart` simples **não** recarrega
   Docker Secrets, precisa recriar o container.
3. Testar com um cadastro novo e confirmar que o e-mail chega.

## Efeito colateral positivo
Motivou a criação de `/panel/logs` (ADMIN/TECNICO) — captura automática de todo WARN/ERROR do
sistema via appender do Logback, persistido no banco (30 dias de retenção), pra não depender de
acaso (ou de alguém abrir `docker logs` na hora certa) pra descobrir um problema desses de novo.

## Ligado a
- Achado durante a homologação de MFA/lista de espera/NPS de 27/08/2026 — impediu terminar de
  validar lista de espera e NPS ao vivo (ambos dependem de e-mail).
- [[painel-de-logs-do-sistema]] — efeito colateral positivo (ver acima).
- [[waitlist-oferta-nao-persistia-after-commit]] — o SMTP quebrado foi, ironicamente, o que expôs
  esse segundo bug (o e-mail de oferta "não chegar" seria indistinguível de "a vaga não foi
  reservada" se o SMTP estivesse funcionando).
