---
tipo: bug
data: 2026-08-27
severidade: Alta
status: Resolvido (01/09/2026) — nova senha de app gerada após recuperação da conta
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

## Causa raiz real (confirmada 01/09/2026): sequestro da conta Google, não expiração comum
A causa não foi rotação de senha inocente. Ao entrar em `myaccount.google.com/security` com o
cliente, o histórico de "Atividades de segurança recentes" mostrou **conta restaurada** no mesmo
dia (via fluxo de recuperação do Google) e, checando "Verificação em duas etapas" → "Senhas de
app", havia uma senha de app cadastrada com nome **"sistema melvin"** — projeto que não tem
nenhuma relação com esta conta (`institutolucas9@gmail.com`); confirmado por busca no repositório
de Sistema Melvin, que usa uma conta de e-mail totalmente diferente (`imeh@igrejadapaz.com.br`,
`~/Applications/sistema_melvin/.env`). Essa senha de app suspeita foi **criada em 14/mai** (mesmo
dia em que senha principal, telefone de recuperação e verificação em duas etapas da conta mudaram
todos juntos — assinatura clássica de conta comprometida) e **usada pela última vez em 30/jun**,
data que bate com o pico de centenas de e-mails de bounce/"Renewal Notice" que lotaram a caixa de
entrada — muito provavelmente o invasor usando o SMTP autenticado da conta pra disparar spam em
massa. A troca da senha principal em 14/mai revogou a senha de app legítima que o Sistema Lucas
usava, o que explica a autenticação falhando desde então (~3,5 meses, não só a última semana).

**Não houve indício de que o incidente tenha exposto dado de paciente do Sistema Lucas** — a conta
comprometida é só a caixa de e-mail usada para envio (`institutolucas9@gmail.com`), sem acesso ao
banco de dados ou à aplicação; o invasor não tinha caminho de lá pra cá.

## Solução aplicada (01/09/2026)
1. Cliente recuperou a conta Google via fluxo de recuperação (`Conta restaurada`) e reconfigurou
   telefone de recuperação e verificação em duas etapas com dados próprios.
2. Apagada a senha de app suspeita ("sistema melvin") antes de gerar qualquer coisa nova.
3. Gerada uma senha de app nova, nomeada "Sistema Lucas".
4. Atualizado `secrets/mail_password.txt` na VPS com a senha nova, **sem espaços** (16 bytes).
5. `docker compose up -d --force-recreate backend` — um `docker restart` simples **não** recarrega
   Docker Secrets, precisa recriar o container.
6. Testado com um cadastro novo (`POST /auth/register`) — sem nenhum `ERROR` de `EmailService` no
   `/panel/logs` nem no `docker logs`, ao contrário de todo teste anterior (100% reprodutível
   antes da correção). Cliente confirmou visualmente o recebimento na caixa de entrada.
7. Checado `myaccount.google.com/permissions` ("Apps vinculados") em busca de acesso de terceiro
   deixado pelo período comprometido — só havia 1 app (Stripe), reconhecido pelo cliente como uso
   legítimo do próprio Instituto. Nenhum resquício de acesso do invasor encontrado.

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
