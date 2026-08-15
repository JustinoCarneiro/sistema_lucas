# Sistema Lucas — Histórias de Usuário e Critérios de Aceite

> Documento reconstruído a partir do código real (Fase 4/5 já em produção) em 29/07/2026, como parte da
> adequação retroativa à metodologia Onda-Dev. Não é uma spec que guiou a construção — é a documentação
> fiel do que já existe. Onde o comportamento real diverge do que seria óbvio/esperado, isso está anotado
> explicitamente.

---

## ÉPICO 1: AUTENTICAÇÃO & SESSÃO

**Escopo:** Login, registro de paciente, JWT + refresh token, logout, verificação de e-mail, recuperação de senha.

#### US-1.1: Login
**Como** usuário cadastrado (qualquer role),
**eu quero** autenticar com e-mail e senha,
**para que** eu receba um token de acesso e acesse o sistema conforme meu perfil.

```gherkin
Dado que o e-mail e a senha informados conferem com um usuário cadastrado,
Quando o usuário faz login,
Então o sistema retorna um cookie HttpOnly "token" (JWT, expira em 15min) e um cookie HttpOnly
  "refresh_token" (expira em 7 dias).

Dado que a senha informada está incorreta,
Quando o usuário tenta logar,
Então o sistema retorna 401, sem revelar se o e-mail existe ou não.
```

#### US-1.2: Registro de Paciente
**Como** visitante,
**eu quero** me cadastrar como paciente,
**para que** eu possa agendar consultas.

```gherkin
Dado que o formulário de registro não expõe nenhum campo de "role",
Quando o cadastro é processado,
Então o usuário é sempre criado com role PATIENT — não há vetor de escalada de privilégio via registro público.

Dado que o e-mail informado já pertence a outro usuário (paciente OU profissional),
Quando o visitante tenta se registrar,
Então o sistema recusa e informa que o e-mail já está em uso — a checagem de unicidade é sobre TODOS os usuários, não só a tabela de pacientes.

Dado que os termos de uso não foram aceitos,
Quando o visitante tenta submeter o registro,
Então o cadastro é bloqueado — aceite é obrigatório e fica registrado com `terms_accepted_at` + `terms_version` (não é só um booleano).
```

#### US-1.3: Logout com Revogação
**Como** usuário logado,
**eu quero** encerrar minha sessão,
**para que** meu token não possa mais ser usado, mesmo que capturado.

```gherkin
Dado que o usuário está autenticado,
Quando ele faz logout,
Então o JWT atual é adicionado à denylist (bloqueado até sua expiração natural, 15min) e o refresh
  token associado é revogado no banco — ambos os cookies são limpos.
```

#### US-1.4: Refresh Token Rotativo
**Como** sistema,
**eu quero** rotacionar o refresh token a cada uso,
**para que** um refresh token vazado só sirva uma única vez.

```gherkin
Dado que o refresh token enviado é válido e não foi usado antes,
Quando o cliente chama /auth/refresh,
Então o token antigo é marcado como usado (não pode ser reaproveitado) e um par novo
  (access + refresh) é emitido.

Dado que o refresh token já foi usado uma vez,
Quando alguém tenta reutilizá-lo,
Então o refresh é recusado.
```

#### US-1.5: Verificação de E-mail
**Como** usuário recém-cadastrado,
**eu quero** confirmar meu e-mail,
**para que** minha conta seja marcada como verificada.

#### US-1.6: Recuperação de Senha
**Como** usuário que esqueceu a senha,
**eu quero** solicitar redefinição por e-mail,
**para que** eu recupere o acesso sem expor se aquele e-mail está cadastrado.

```gherkin
Dado que o e-mail informado existe ou não no sistema,
Quando "esqueci minha senha" é submetido,
Então a resposta é sempre a mesma mensagem de sucesso — não revela a existência da conta.
```

#### US-1.7: Rate Limiting em Rotas Sensíveis
**Como** sistema,
**eu quero** limitar tentativas por IP em rotas críticas,
**para que** força-bruta de login/exportação de dados seja mitigada.

```gherkin
Dado que um IP fez 30 requisições no último minuto a uma rota protegida (/auth/**, /export/**,
  /prontuarios/**, /documentos/**),
Quando a 31ª requisição chega,
Então o sistema retorna 429 com aviso de bloqueio por 1 minuto.
```

---

## ÉPICO 2: GESTÃO DE PERFIL

**Escopo:** Paciente e profissional gerenciam o próprio perfil; admin gerencia profissionais.

#### US-2.1: Paciente Vê/Edita o Próprio Perfil
**Como** paciente,
**eu quero** ver e atualizar meus dados cadastrais,
**para que** minhas informações fiquem corretas.

#### US-2.2: Profissional Vê/Edita o Próprio Perfil
**Como** profissional,
**eu quero** ver e atualizar meus dados (incluindo modalidade de atendimento: presencial/online/híbrido),
**para que** o paciente veja informações atualizadas ao escolher um profissional.

#### US-2.3: Admin Gerencia Profissionais
**Como** administrador,
**eu quero** cadastrar, editar e excluir profissionais,
**para que** a equipe clínica esteja sempre atualizada no sistema.

```gherkin
Dado que um profissional tem consultas ou prontuários vinculados,
Quando o admin tenta excluí-lo pela rota normal,
Então o sistema recusa com mensagem amigável explicando o vínculo.

Dado que o admin realmente precisa remover esse profissional,
Quando ele usa a exclusão forçada (rota separada, "force delete"),
Então o sistema apaga em cascata: documentos → prontuários → disponibilidade → consultas → e só então o profissional.
```

#### US-2.4: Exclusão de Conta de Paciente
**Como** paciente (ou administrador em seu nome),
**eu quero** excluir a conta,
**para que** meus dados pessoais não continuem armazenados desnecessariamente.

> Ver ÉPICO 10 (Segurança & LGPD) para o comportamento real de anonimização quando há vínculo clínico — não é um DELETE simples.

---

## ÉPICO 3: DISPONIBILIDADE & AGENDA DO PROFISSIONAL

**Escopo:** Grade de horários do profissional, por data específica (não por dia-da-semana recorrente).

#### US-3.1: Cadastrar Disponibilidade Mensal
**Como** profissional,
**eu quero** definir meus horários livres para o mês atual ou o próximo,
**para que** pacientes só vejam slots realmente disponíveis.

```gherkin
Dado que o profissional tenta editar a disponibilidade de um mês já passado,
Quando ele submete a grade,
Então o sistema recusa — só o mês atual ou o próximo podem ser editados.

Dado que um horário da grade atual tem uma consulta ativa vinculada (AGUARDANDO_CONFIRMACAO,
  AGENDADA, CONFIRMADA_PROFISSIONAL ou CONFIRMADA),
Quando o profissional tenta salvar uma nova grade que remove esse horário,
Então o sistema recusa e pede para cancelar a consulta individualmente antes.

Dado que um slot é criado,
Então ele sempre dura exatamente 1 hora (`endTime = startTime + 1h`, não configurável por slot).
```

#### US-3.2: Paciente Consulta Disponibilidade
**Como** paciente,
**eu quero** ver quais profissionais têm horário livre e em quais datas/horas,
**para que** eu escolha um slot pra agendar.

```gherkin
Dado que um slot já está ocupado por qualquer consulta não-cancelada, ou já passou no relógio
  (fuso America/Sao_Paulo),
Quando o paciente consulta os horários livres de um profissional num dia,
Então esse slot não aparece na lista.
```

---

## ÉPICO 4: AGENDAMENTO DE CONSULTAS

**Escopo:** A máquina de estados completa da consulta — do agendamento à conclusão/cancelamento/falta.

#### US-4.1: Agendar Consulta
**Como** paciente,
**eu quero** agendar uma consulta num slot livre,
**para que** eu seja atendido pelo profissional escolhido.

```gherkin
Dado que o paciente está bloqueado por penalidade (`blockedUntil` no futuro — ver ÉPICO 7),
Quando ele tenta agendar,
Então o sistema recusa e informa a data em que o bloqueio termina.

Dado que o agendamento foi aceito,
Então a consulta nasce no estado AGUARDANDO_CONFIRMACAO — ainda não é AGENDADA até o profissional aprovar.
```

#### US-4.2: Profissional Aprova ou Recusa
**Como** profissional,
**eu quero** aprovar ou recusar uma solicitação de consulta,
**para que** só consultas realmente viáveis avancem na minha agenda.

```gherkin
Dado que a consulta está em AGUARDANDO_CONFIRMACAO,
Quando o profissional aprova,
Então o estado vira AGENDADA.

Dado que a consulta está em AGUARDANDO_CONFIRMACAO,
Quando o profissional recusa (com justificativa obrigatória),
Então o estado vira CANCELADA.

Dado que a consulta NÃO está mais em AGUARDANDO_CONFIRMACAO,
Quando alguém tenta aprovar ou recusar de novo,
Então o sistema recusa com "Apenas consultas aguardando confirmação podem ser aprovadas/recusadas."
```

#### US-4.3: Dupla Confirmação
**Como** profissional e depois paciente,
**eu quero** confirmar a consulta em duas etapas,
**para que** ambos os lados reafirmem o compromisso antes da data.

```gherkin
Dado que a consulta está AGENDADA,
Quando o profissional confirma,
Então o estado vira CONFIRMADA_PROFISSIONAL.

Dado que a consulta está CONFIRMADA_PROFISSIONAL,
Quando o paciente confirma,
Então o estado vira CONFIRMADA.

Dado que a consulta ainda está só AGENDADA (profissional não confirmou),
Quando o paciente tenta confirmar primeiro,
Então o sistema recusa: "Aguardando confirmação do profissional primeiro."

Nota: não existe mais trava de antecedência mínima pra confirmar — foi removida deliberadamente
  (comentário no código: deve ser possível confirmar a qualquer momento antes da consulta).
```

#### US-4.4: Cancelar Consulta
**Como** paciente, profissional (dono) ou administrador,
**eu quero** cancelar uma consulta em qualquer estado,
**para que** compromissos que não vão mais acontecer sejam encerrados.

```gherkin
Dado que uma justificativa não foi informada,
Quando qualquer um tenta cancelar,
Então o sistema recusa — justificativa é sempre obrigatória.

Dado que a consulta cancelada já havia passado por AGUARDANDO_CONFIRMACAO e falta menos de 24h
  pro horário marcado,
Quando o cancelamento é confirmado,
Então uma penalidade é aplicada ao paciente (ver ÉPICO 7).

Dado que restam 24h ou mais pro horário marcado, OU a consulta ainda estava em
  AGUARDANDO_CONFIRMACAO,
Quando o cancelamento é confirmado,
Então NENHUMA penalidade é aplicada.
```

#### US-4.5: Reagendar Consulta
**Como** paciente,
**eu quero** mudar a data/hora de uma consulta já marcada,
**para que** eu não precise cancelar e agendar de novo do zero.

```gherkin
Dado que a consulta está em qualquer estado (inclusive já CONFIRMADA por ambos),
Quando o paciente reagenda pra uma nova data/hora,
Então o estado sempre volta pra AGENDADA — o ciclo de dupla confirmação reinicia, mesmo que já
  estivesse totalmente confirmada antes.
```

#### US-4.6: Marcar Falta
**Como** profissional,
**eu quero** registrar que o paciente não compareceu,
**para que** o sistema aplique a penalidade correspondente.

```gherkin
Dado que o paciente não compareceu,
Quando o profissional marca falta,
Então o estado vira FALTA e a penalidade é aplicada ao paciente **sempre**, independente de
  qualquer janela de 24h (diferente do cancelamento tardio).
```

#### US-4.7: Lembrete de Consulta via WhatsApp
**Status:** 🔲 Backlog — aprovado pelo cliente em 10/08/2026, ainda não desenvolvido.

**Como** paciente,
**eu quero** receber um lembrete da minha consulta também por WhatsApp, além do e-mail que já recebo,
**para que** eu não esqueça o horário — estudos mostram que lembrete por WhatsApp reduz falta em consulta entre 30% e 69%.

```gherkin
Dado que uma consulta está confirmada pra amanhã e o paciente tem telefone/WhatsApp cadastrado,
Quando o `LembreteScheduler` roda (mesmo job diário às 10h que já dispara o lembrete por e-mail),
Então uma mensagem de WhatsApp equivalente é disparada pro número cadastrado, além do e-mail.

Dado que o envio por WhatsApp falha (API fora do ar, número inválido etc.),
Quando isso acontece,
Então o lembrete por e-mail continua sendo enviado normalmente — falha no WhatsApp nunca deve
  quebrar o canal que já funciona.
```

> **Nota de implementação:** plugar no mesmo gatilho do lembrete por e-mail já existente —
> `LembreteScheduler` (`@Scheduled(cron = "0 0 10 * * *")`) → `EmailTemplateService.enviarLembrete()`.
> O campo `Patient.phone` já existe e já é rotulado "WhatsApp" no formulário de cadastro do
> frontend, mas hoje é usado só como referência de contato humano — nenhuma integração de envio
> existe. Precisa de client novo (`WhatsAppService` ou similar) plugado ao lado do `EmailService`,
> não substituindo-o.
>
> **Nota de custo (levantamento 10/08/2026):** não é gratuito, mas também não é caro por mensagem
> — lembrete de consulta se enquadra em categoria "utilidade" (mensagem transacional ligada a um
> evento), tabelada em ~R$0,04–0,09 por envio no Brasil em 2026 via API oficial (Meta/WhatsApp
> Business Platform). O custo que pesa de verdade não é a Meta, é a **mensalidade de um
> provedor/BSP** (Twilio, Zenvia, 360dialog etc.) — R$200–1.200/mês no Brasil — necessária pra
> acessar a API oficial. Existe alternativa não-oficial sem mensalidade (bibliotecas tipo
> Baileys/whatsapp-web.js), mas ela **viola os Termos de Uso do WhatsApp** (risco de banimento do
> número, sem suporte oficial) — não recomendada pra uso clínico sem esse risco estar explícito
> pro cliente. Zero integração de WhatsApp/Twilio/BSP existe hoje no repositório.

#### US-4.8: Lista de Espera para Cancelamentos
**Status:** 🔍 Implementado em 15/08/2026 (versão robusta), testes unitários verdes (backend e
frontend) — aguardando code review e homologação com o cliente. Ainda não deployado em produção.

**Como** paciente que não encontrou horário livre com um profissional,
**eu quero** entrar numa lista de espera pra um profissional/horário específico,
**para que** eu seja avisado automaticamente se uma vaga abrir por cancelamento, sem precisar
ficar checando o sistema manualmente.

**Decisão de produto resolvida (15/08/2026):** confirmação ativa — a vaga é reservada de fato
pro primeiro da fila (não é "quem chega primeiro leva"). Isso repriced o módulo de 🟡 Médio pra
🔴 Grande, exatamente como a nota de produto original já antecipava.

```gherkin
Dado que não há slot livre pro profissional/data desejados,
Quando o paciente entra na lista de espera pra esse profissional + horário,
Então o pedido de espera é registrado, vinculado ao paciente (status AGUARDANDO).

Dado que existe alguém na lista de espera pra um profissional/horário específicos,
Quando uma consulta com essa mesma chave (professionalId + dateTime) é cancelada
  (`AppointmentService.cancelar`),
Então o primeiro da fila recebe uma consulta reservada em seu nome (status AGUARDANDO_CONFIRMACAO,
  ocupando o horário de verdade) e um e-mail com um link de confirmação de uso único, válido por
  2 horas (status da entrada na fila vira OFERECIDA).

Dado que o paciente confirma a vaga dentro do prazo,
Quando ele clica no link e confirma,
Então a entrada na fila vira CONFIRMADA e a consulta reservada segue o ciclo normal — inclusive
  precisa da aprovação do profissional, como qualquer outra consulta.

Dado que o paciente não confirma dentro do prazo,
Quando o prazo expira,
Então a consulta reservada é cancelada automaticamente (sem penalidade — quem cancelou foi o
  sistema, não o paciente), a entrada na fila vira EXPIRADA, e a vaga é oferecida ao próximo da
  fila (mesmo fluxo, recursivo).

Dado que o próximo da fila está bloqueado por penalidade no momento em que a vaga seria ofertada,
Quando o sistema tenta ofertar a ele,
Então ele é pulado (entrada vira CANCELADA) e a vaga passa pro próximo, sem notificá-lo.
```

> **Nota de implementação (como foi construído):** `AppointmentService.cancelar()` publica um
> evento (`ConsultaCanceladaEvent`) em vez de chamar `WaitlistService` diretamente —
> `AppointmentService` e `WaitlistService` dependerem um do outro nos dois sentidos criaria uma
> dependência circular que o Spring Boot recusa resolver por padrão
> (`spring.main.allow-circular-references=false`, o padrão desde o Spring Boot 2.6). O
> `WaitlistService` escuta via `@TransactionalEventListener(phase = AFTER_COMMIT)` — só oferece a
> vaga depois que o cancelamento realmente foi persistido. Entidade nova `WaitlistEntry`
> (migration V16): profissional, paciente, `dateTime`, `status` (AGUARDANDO/OFERECIDA/
> CONFIRMADA/EXPIRADA/CANCELADA), `appointmentId` (a consulta reservada), `token`,
> `ofertaExpiraEm`. Expiração automática via `WaitlistExpirationScheduler`
> (`@Scheduled`, a cada 15min). Prazo de confirmação configurável
> (`app.waitlist.oferta.horas`, padrão 2h).

---

## ÉPICO 5: PRONTUÁRIO ELETRÔNICO

**Escopo:** Registro clínico da consulta — é o evento que efetivamente conclui o atendimento.

#### US-5.1: Criar Prontuário
**Como** profissional,
**eu quero** registrar as notas clínicas de uma consulta realizada,
**para que** o histórico do paciente fique documentado.

```gherkin
Dado que o profissional cria um prontuário vinculado a uma consulta,
Quando o prontuário é salvo,
Então a consulta correspondente é automaticamente marcada como CONCLUIDA — este é o único
  lugar do sistema que atribui esse status (não existe um endpoint "concluir consulta" separado).

Nota: não há guarda de estado prévio nesse método hoje — tecnicamente um prontuário pode ser
  criado (e a consulta concluída) a partir de qualquer estado atual, inclusive
  AGUARDANDO_CONFIRMACAO. Vale avaliar se isso deveria ter uma guarda (ex.: só a partir de
  CONFIRMADA) — registrado como candidato a decisão técnica, não uma correção feita agora.
```

#### US-5.2: Consultar Histórico de Prontuários
**Como** profissional ou administrador,
**eu quero** ver o histórico de prontuários de um paciente,
**para que** o acompanhamento clínico tenha contexto.

---

## ÉPICO 6: DOCUMENTOS CLÍNICOS

**Escopo:** Upload e gestão de documentos (laudos, atestados, encaminhamentos) vinculados ao paciente.

#### US-6.1: Criar Documento
**Como** profissional,
**eu quero** anexar um documento (texto ou PDF) a um paciente,
**para que** laudos e atestados fiquem centralizados no sistema.

```gherkin
Dado que o conteúdo enviado é um PDF em base64,
Quando o documento é submetido,
Então o sistema valida a assinatura de magic bytes ("JVBERi0"/"JVBERiA", correspondente a "%PDF-")
  antes de aceitar — um arquivo disfarçado de PDF é rejeitado.

Dado que o base64 enviado excede ~5MB de arquivo real (7.000.000 caracteres em base64),
Quando o documento é submetido,
Então o sistema rejeita por tamanho.

Dado que o documento é criado,
Então ele nasce com `disponivel=false` — invisível ao paciente até o profissional liberar
  explicitamente.
```

#### US-6.2: Controlar Visibilidade do Documento
**Como** profissional,
**eu quero** decidir quando um documento fica visível ao paciente,
**para que** eu só libere depois de revisar o conteúdo.

#### US-6.3: Paciente Vê Seus Documentos
**Como** paciente,
**eu quero** ver os documentos que o profissional liberou pra mim,
**para que** eu tenha acesso a laudos e atestados.

---

## ÉPICO 7: PENALIDADES POR FALTA/CANCELAMENTO TARDIO

**Escopo:** Sistema progressivo de advertência e bloqueio por comportamento do paciente.

#### US-7.1: Primeira Infração — Advertência
**Como** sistema,
**eu quero** avisar o paciente na primeira falta/cancelamento tardio,
**para que** ele tenha uma chance antes de qualquer bloqueio.

```gherkin
Dado que é a primeira infração do paciente (nunca recebeu advertência antes),
Quando uma falta ou cancelamento tardio (<24h) ocorre,
Então o contador de infrações incrementa, um e-mail de advertência é enviado, e o paciente
  NÃO é bloqueado ainda.
```

#### US-7.2: Segunda Infração — Bloqueio de 15 Dias
**Como** sistema,
**eu quero** bloquear temporariamente o paciente após a 2ª infração,
**para que** o comportamento recorrente tenha consequência real.

```gherkin
Dado que o paciente já havia recebido a advertência da primeira infração,
Quando uma nova falta ou cancelamento tardio ocorre,
Então `blockedUntil` é definido para 15 dias a partir de agora, um e-mail de bloqueio é enviado,
  e novos agendamentos são recusados até essa data (ver US-4.1).
```

#### US-7.3: Desbloqueio Administrativo
**Como** administrador,
**eu quero** desbloquear um paciente manualmente,
**para que** casos excepcionais possam ser resolvidos sem esperar os 15 dias.

```gherkin
Dado que um paciente está bloqueado,
Quando o admin aciona o desbloqueio,
Então TODO o histórico de penalidade é zerado (bloqueio, contador de infrações e a flag de
  primeira advertência) — não é um desbloqueio parcial, é reinício completo.
```

---

## ÉPICO 8: EXPORTAÇÃO DE DADOS & PORTABILIDADE LGPD

**Escopo:** Exportação de dados por role, incluindo o direito de portabilidade do paciente.

#### US-8.1: Admin Exporta Dados Operacionais
**Como** administrador,
**eu quero** exportar CSV de consultas, pacientes (CPF mascarado) e profissionais,
**para que** eu tenha dados pra análise externa.

#### US-8.2: Profissional Exporta Prontuários
**Como** profissional,
**eu quero** exportar CSV dos prontuários que criei,
**para que** eu tenha backup próprio do meu trabalho clínico.

#### US-8.3: Paciente Exporta os Próprios Dados (Portabilidade)
**Como** paciente,
**eu quero** baixar todos os meus dados em formato estruturado,
**para que** eu exerça meu direito de portabilidade (LGPD Art. 18, V).

```gherkin
Dado que o paciente solicita a exportação dos próprios dados,
Quando o export é gerado,
Então o formato é JSON (não CSV) e inclui dados cadastrais + prontuários + documentos +
  consultas + metadados do consentimento LGPD registrado no cadastro.
```

---

## ÉPICO 9: PAINEL ADMINISTRATIVO

**Escopo:** Dashboards com métricas operacionais, uma visão por role.

#### US-9.1: Dashboard do Administrador
**Como** administrador,
**eu quero** ver totais de profissionais/pacientes e consultas do dia por status,
**para que** eu tenha visão geral da operação.

#### US-9.2: Dashboard do Profissional
**Como** profissional,
**eu quero** ver minha agenda de hoje, pendências de confirmação, atrasadas, próximas consultas
e pacientes únicos atendidos,
**para que** eu organize meu dia de trabalho.

#### US-9.3: Dashboard do Paciente
**Como** paciente,
**eu quero** ver minha próxima consulta, pendências de confirmação e documentos disponíveis,
**para que** eu saiba o que preciso fazer.

---

## ÉPICO 10: SEGURANÇA & CONFORMIDADE LGPD

**Escopo:** Transversal — não é uma tela, é um conjunto de garantias que atravessa todos os épicos acima.

#### US-10.1: Criptografia de Campo Sensível
**Como** sistema,
**eu quero** criptografar todo campo de dado sensível em repouso,
**para que** um vazamento do banco não exponha CPF, contato, alergias ou conteúdo clínico em texto plano.

```gherkin
Dado que um campo sensível (CPF, telefone, endereço, contato de emergência, alergias, motivo de
  consulta, notas de prontuário, conteúdo de documento) é salvo,
Quando o valor é persistido,
Então ele é armazenado como AES-256-GCM com IV aleatório por valor — nunca em texto plano.

Dado que existe dado histórico cifrado com uma chave antiga (ou em texto plano, pré-migração),
Quando o sistema tenta ler esse campo,
Então ele tenta a chave atual e, se falhar, cada chave legada configurada, com um fallback
  final pra AES-128-ECB — sem isso, dado antigo ficaria ilegível após rotação de chave.
```

#### US-10.2: Hash de CPF para Deduplicação
**Como** sistema,
**eu quero** checar unicidade de CPF sem nunca comparar em texto plano,
**para que** a deduplicação não exponha o dado nem seja atacável por força bruta.

```gherkin
Dado que um CPF cifrado não é pesquisável diretamente (IV aleatório por valor),
Quando o sistema precisa checar se um CPF já existe,
Então a busca usa um índice HMAC-SHA256 com pepper secreto (`cpf_hash`) — não o SHA-256 puro
  usado na primeira versão da migration, que foi considerado vulnerável a força bruta dado o
  espaço limitado de CPFs válidos.
```

#### US-10.3: Anonimização na Exclusão de Paciente
**Como** sistema,
**eu quero** anonimizar em vez de apagar fisicamente quando há vínculo clínico,
**para que** a retenção legal de prontuário (CFM, 20 anos) seja respeitada junto com o direito
ao esquecimento (LGPD Art. 18).

```gherkin
Dado que o paciente não tem nenhuma consulta nem prontuário vinculado,
Quando a exclusão é solicitada (pelo próprio ou por admin),
Então o registro é apagado fisicamente do banco.

Dado que o paciente TEM consulta ou prontuário vinculado,
Quando a exclusão é solicitada,
Então o registro não é apagado — nome, e-mail, telefone, endereço, alergias, contato de
  emergência, data de nascimento, gênero e CPF são irreversivelmente sobrescritos, a senha vira
  um hash de valor aleatório (login impossível), `isActive=false`, e o histórico de penalidades
  é zerado. O prontuário em si permanece intacto.

Dado que um vínculo clínico é criado exatamente entre a checagem e a exclusão física (corrida),
Quando a exclusão colide com esse vínculo novo,
Então o sistema captura o erro de integridade e recua pra anonimização em vez de falhar.
```

#### US-10.4: Auditoria de Acesso
**Como** sistema,
**eu quero** registrar toda operação sensível,
**para que** haja trilha auditável de quem acessou/alterou o quê.

---

## ÉPICO 11: SATISFAÇÃO DO PACIENTE (NPS)

**Escopo:** Coleta estruturada de feedback do paciente após o atendimento. Novo em 10/08/2026 —
aprovado pelo cliente a partir de proposta de evolução (benchmark com institutos/clínicas
similares + estudos sobre agendamento em saúde). Implementado em 15/08/2026.

#### US-11.1: NPS Pós-Consulta
**Status:** 🔍 Implementado em 15/08/2026, testes unitários verdes (backend e frontend) — aguardando code review e homologação com o cliente. Ainda não deployado em produção.

**Decisão de produto resolvida:** o link de avaliação é público e de uso único (token na URL,
sem exigir login) — mesmo padrão já usado em `VerificationToken`/`PasswordResetToken` neste
projeto. Mitigação de abuso: rota `/nps/**` entrou no rate limiting (30 req/min por IP, mesma
faixa de `/auth/**`), token de 128 bits (`UUID.randomUUID()`) e expiração de 7 dias.

**Como** administrador,
**eu quero** que o paciente receba automaticamente um pedido de nota de 0 a 10 depois que a
consulta é concluída,
**para que** o Instituto tenha um retorno estruturado da experiência do paciente — hoje não existe
nenhum mecanismo de feedback.

```gherkin
Dado que uma consulta muda pra CONCLUIDA (efeito colateral de `ProntuarioService.create()`,
  único ponto do sistema que conclui uma consulta),
Quando essa transição acontece,
Então um pedido de avaliação (nota 0-10 + comentário opcional) é disparado ao paciente por e-mail,
  de forma assíncrona, sem bloquear a criação do prontuário.

Dado que o paciente responde a avaliação,
Quando ele submete a nota,
Então ela é salva vinculada à consulta e ao paciente (não ao prontuário — não deve tocar dado
  clínico sensível).

Dado que o paciente não responde,
Quando qualquer prazo (a definir) se esgota,
Então nada quebra — a ausência de resposta é um estado válido, não um erro.
```

> **Nota de implementação (como foi construído):** plugado em `ProntuarioService.create()`, o
> único gatilho de `CONCLUIDA` no sistema, dentro de uma transação própria
> (`@Transactional(propagation = REQUIRES_NEW)`, em `NpsService.solicitarAvaliacao`) — isolar a
> transação, e não só o `try/catch`, é o que de fato impede que uma falha no pedido de NPS marque
> a transação da conclusão da consulta como rollback-only. Reaproveita
> `EmailService`/`EmailTemplateService` (novo método `solicitarAvaliacaoNps`, mesmo padrão do
> lembrete de consulta). Entidade nova `NpsResponse` (migration V15): `appointmentId` (FK única),
> `patientId`, `token`, `score`, `comentario`, `criadoEm`, `respondidoEm`, `expiraEm`. Idempotente
> — se já existe um pedido de NPS pra aquela consulta, não duplica nem reenvia e-mail. Cálculo do
> NPS agregado (% promotores − % detratores) para o Dashboard do Administrador (E9) não entrou
> neste escopo — fica como próximo passo, não fez parte do mínimo aprovado.
