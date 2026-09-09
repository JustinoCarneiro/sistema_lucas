# Manual de Uso — Sistema Lucas

> Manual operacional, passo a passo, para os perfis de acesso do sistema: **Paciente**,
> **Profissional**, **Administrador** e **Técnico** (o Técnico tem o mesmo acesso do
> Administrador — ver [4.9](#49-perfil-técnico)). Descreve o que cada tela faz e como usá-la — não
> é a especificação técnica (isso está em [`docs/spec.md`](./spec.md)) nem o guia de conformidade
> LGPD voltado ao público (isso está em
> [`documentacao/documentacao_cliente.html`](../documentacao/documentacao_cliente.html), com foco
> em proteção de dados). Escrito a partir do comportamento real das telas em 03/08/2026;
> revisado em 09/09/2026 para incluir verificação em duas etapas, lista de espera, avaliação
> pós-consulta (NPS), o perfil Técnico e o painel de logs.

## Sumário

1. [Acesso ao sistema (comum a todos os perfis)](#1-acesso-ao-sistema-comum-a-todos-os-perfis)
2. [Manual do Paciente](#2-manual-do-paciente)
3. [Manual do Profissional](#3-manual-do-profissional)
4. [Manual do Administrador](#4-manual-do-administrador)
5. [Máquina de estados da consulta — referência rápida](#5-máquina-de-estados-da-consulta--referência-rápida)
   - [5.1 Diagrama de estados](#51-diagrama-de-estados)
   - [5.2 Cancelar, recusar e reagendar — quem faz o quê e onde](#52-cancelar-recusar-e-reagendar--quem-faz-o-quê-e-onde)
6. [Perguntas frequentes gerais](#6-perguntas-frequentes-gerais)

---

## 1. Acesso ao sistema (comum a todos os perfis)

### 1.1 Criar conta (somente paciente)

Só o **paciente** se cadastra sozinho, na tela **Cadastro** (link a partir da tela de Login).
Profissionais e administradores não se auto-cadastram — a conta deles é criada por um
administrador (ver [4.2](#42-profissionais--cadastrar-editar-e-excluir)).

Passos:
1. Preencha nome, e-mail, senha e demais dados pedidos.
2. Aceite os **Termos de Uso e a Política de Privacidade** — o aceite é obrigatório; sem ele o
   cadastro não é enviado.
3. Envie o formulário.
4. Você receberá um e-mail de **verificação**, com um link válido por 24 horas. Clique nele para
   ativar a conta.

Se o e-mail informado já estiver em uso — como paciente ou como profissional — o sistema recusa o
cadastro e avisa que o e-mail já está cadastrado.

> **Ainda não verifiquei meu e-mail — posso usar o sistema?** Sim. Enquanto a conta não é
> verificada, aparece uma faixa amarela no topo ("Seu e-mail ainda não foi verificado. Por favor,
> verifique sua caixa de entrada."). Você consegue fazer login e navegar normalmente; a faixa some
> assim que você clica no link do e-mail.
>
> **O link de verificação expirou (mais de 24h) e não recebi outro.** O sistema não tem um botão
> de "reenviar verificação". Nesse caso, contate a administração da clínica — ela consegue
> reenviar o e-mail ou confirmar a conta manualmente. Verifique antes a caixa de spam/lixo
> eletrônico.

### 1.2 Login

Na tela **Login**, informe e-mail e senha. Se as credenciais não conferirem, o sistema mostra um
erro genérico (não informa se o problema foi o e-mail ou a senha, por segurança).

Sua sessão fica ativa por um período limitado; ao expirar, o sistema pede login novamente.

### 1.3 Esqueci minha senha

Na tela de login, use o link **Esqueci minha senha**:
1. Informe o e-mail cadastrado.
2. O sistema sempre mostra a mesma mensagem de sucesso, exista ou não aquele e-mail na base —
   isso é proposital, para não revelar quais e-mails têm conta.
3. Se o e-mail existir, chega um link de redefinição, de uso único e validade curta.
4. Abra o link, defina a nova senha.

### 1.4 Sair do sistema

No rodapé do menu lateral, o botão **Sair do sistema** encerra a sessão. O token de acesso é
invalidado imediatamente no servidor (não basta fechar a aba — ver [Segurança](#66-segurança-da-sessão)).

### 1.5 Tema claro/escuro

O ícone de sol/lua no topo da tela alterna entre tema claro e escuro. A preferência fica salva
para os próximos acessos.

### 1.6 Verificação em duas etapas (2FA)

A verificação em duas etapas está disponível para **Administrador**, **Profissional** e
**Paciente**. Quando ativada, o login passa a exigir, além da senha, um **código de 6 dígitos** de
um aplicativo autenticador (Google Authenticator, Authy e similares).

**Como ativar:**
1. Abra **Meu Perfil** (Paciente/Profissional) ou **Segurança** (Administrador/Técnico).
2. Clique em **Ativar verificação em duas etapas**.
3. Escaneie o QR code com o aplicativo autenticador — ou digite o código exibido manualmente.
4. Digite o código de 6 dígitos que o aplicativo gerou e confirme.
5. O sistema mostra os **códigos de backup** — anote-os num lugar seguro. Cada código funciona
   **uma única vez** e serve para entrar caso você perca o acesso ao aplicativo. **Eles não são
   exibidos de novo.**

**Como é o login depois de ativada:** você informa e-mail e senha normalmente; em seguida o
sistema pede o código do aplicativo. Só depois do código a sessão é aberta. Perdeu o celular? Use
um dos códigos de backup no lugar do código do aplicativo.

**Como desativar:** na mesma tela, clique em **Desativar** e informe sua senha e um código
(do aplicativo ou de backup). Ao desativar, todas as suas sessões abertas são encerradas.

### 1.7 Sessão expirada por inatividade

Depois de **15 minutos sem nenhuma ação** na área logada, o sistema encerra a sessão
automaticamente e volta para a tela de login. É uma proteção para computadores compartilhados
(recepção, consultório). Basta entrar de novo — nada é perdido.

### 1.8 Trocar a senha estando logado

**Paciente** e **Profissional**: em **Meu Perfil**, botão **Alterar minha senha** — informe a
nova senha duas vezes (mínimo de 6 caracteres). **Administrador/Técnico** não têm "Meu Perfil";
se precisarem trocar a senha, usam o fluxo **Esqueci minha senha** ([1.3](#13-esqueci-minha-senha)).

---

## 2. Manual do Paciente

Menu lateral ("Minha Área"): **Início · Minhas Consultas · Meus Documentos**, com **Meu Perfil**
no rodapé.

### 2.1 Meu Perfil

Tela para ver e atualizar seus próprios dados cadastrais (nome, telefone, endereço, contato de
emergência, alergias etc.) a qualquer momento.

### 2.2 Início (Painel)

Mostra, num só lugar:
- sua **próxima consulta**;
- consultas que estão **aguardando alguma ação sua** (ex.: confirmar presença);
- **documentos** que o profissional já liberou para você.

### 2.3 Agendar uma consulta

Em **Minhas Consultas**, use o formulário de agendamento:

| Passo | Ação |
|---|---|
| 1 | Escolha o **profissional** — a lista mostra a modalidade de atendimento de cada um (presencial, online ou híbrido). |
| 2 | Escolha a **data**. |
| 3 | Escolha um **horário livre** entre os disponíveis naquele dia — horários ocupados ou já passados não aparecem. |
| 4 | Descreva, opcionalmente, o **motivo da consulta**. |
| 5 | Confirme. A consulta é criada no status **Aguardando Confirmação**. |

Se você estiver **bloqueado por penalidade** (ver [2.9](#29-penalidades-por-falta-ou-cancelamento-tardio)),
o sistema recusa o agendamento e informa a data em que o bloqueio termina.

### 2.4 Acompanhar o status da consulta

Cada consulta listada em **Minhas Consultas** mostra um status:

| Status na tela | O que significa | O que fazer |
|---|---|---|
| **Aguardando Confirmação** | Você agendou, falta o profissional aceitar. | Aguardar. |
| **Agendada** | O profissional aceitou. | Aguardar o profissional confirmar (etapa seguinte da dupla confirmação). |
| **Aguardando paciente** | O profissional já confirmou; falta você confirmar. | Confirmar sua presença (ver [2.5](#25-confirmar-presença)). |
| **Confirmada** | Ambos os lados confirmaram. | Comparecer na data marcada. |
| **Concluída** | O profissional já atendeu e registrou o prontuário. | — |
| **Cancelada** | A consulta foi cancelada (por você, pelo profissional ou por um admin). | — |
| **Faltou** | Você não compareceu e o profissional marcou falta. | — |

### 2.5 Confirmar presença

A consulta passa por **duas confirmações** antes do dia do atendimento: primeiro o profissional,
depois você. Você só consegue confirmar depois que o profissional já confirmou (status
**Aguardando paciente**) — se tentar antes, o sistema avisa: *"Aguardando confirmação do
profissional primeiro."*

### 2.6 Cancelar uma consulta

Em **Minhas Consultas**, o botão **Cancelar** aparece em toda consulta que ainda não foi
encerrada — inclusive nas que ainda estão "Aguardando Confirmação". Ao cancelar:
- é obrigatório escrever uma **justificativa** (mínimo de 10 caracteres);
- se faltar **menos de 24h** para o horário marcado, **e** a consulta ainda estiver no futuro,
  **e** ela já tiver passado da fase "Aguardando Confirmação", uma penalidade é aplicada à sua
  conta (ver [2.9](#29-penalidades-por-falta-ou-cancelamento-tardio));
- cancelamentos com 24h ou mais de antecedência, ou de consultas ainda não aceitas pelo
  profissional, **não** geram penalidade.

> A janela de cancelamento mostra um aviso de penalidade sempre que faltam menos de 24h para o
> horário (ou a data já passou). Esse aviso é preventivo: se a consulta **já passou da data**, o
> cancelamento **não** bloqueia sua conta — a penalidade só vale para consulta que ainda vai
> acontecer.

### 2.7 Reagendar uma consulta

O botão **Reagendar** só aparece depois que o profissional **já aceitou** a consulta (status
"Agendada" ou adiante). Se ela ainda está "Aguardando Confirmação", cancele e agende de novo.

Ao reagendar para nova data/horário, a consulta **volta para "Agendada"** e o ciclo de dupla
confirmação recomeça daí — o profissional confirma de novo e depois você — mesmo que ela já
estivesse totalmente confirmada antes. Reagendar exige **justificativa** e, se a consulta original
estiver a menos de 24h, aplica a mesma penalidade do cancelamento tardio.

### 2.8 Meus Documentos

Nesta tela você vê os documentos clínicos (laudos, atestados, exames) que o profissional
**liberou** para você. Documentos ainda não liberados pelo profissional não aparecem. Documentos
em PDF podem ser baixados pelo botão **Baixar PDF**.

### 2.9 Penalidades por falta ou cancelamento tardio

O sistema aplica penalidade progressiva quando você falta a uma consulta ou cancela com menos de
24h de antecedência (depois que a consulta já havia sido aceita pelo profissional):

1. **1ª ocorrência:** advertência por e-mail. Sua conta **não** é bloqueada.
2. **Da 2ª ocorrência em diante:** sua conta fica **bloqueada por 15 dias** para novos
   agendamentos, com aviso por e-mail. Cada nova infração após a advertência reinicia um bloqueio
   de 15 dias.

Enquanto estiver bloqueado, ao tentar agendar o sistema recusa e informa **a data e a hora em que
o bloqueio termina**. Passado esse prazo, o agendamento volta a funcionar sozinho.

Uma falta marcada pelo profissional (você não compareceu) **sempre** gera penalidade, mesmo que a
data já estivesse próxima ou distante — não há regra de antecedência para falta, só para
cancelamento.

Se precisar de desbloqueio antes do prazo, contate a administração (ver [4.3](#43-pacientes--visualizar-desbloquear-e-excluir)).

### 2.10 Portabilidade — exportar meus dados

Na tela **Meus Documentos**, o botão **Portabilidade (Exportar Meus Dados)** gera um arquivo
**JSON** com todos os seus dados: cadastro, prontuários, documentos, consultas e o registro do seu
consentimento aos Termos de Uso — seu direito de portabilidade garantido pela LGPD (Art. 18, V).

### 2.11 Lista de Espera

Quando o profissional não tem horário livre na data que você quer, use a **Lista de Espera**
(menu lateral):

1. Escolha o **profissional**, a **data** e o **horário** que você gostaria.
2. Clique em **Entrar na fila**.
3. Se alguém cancelar ou reagendar e liberar exatamente aquele horário, o sistema **reserva a vaga
   para o primeiro da fila** e envia um **e-mail com um link de confirmação**.
4. Você tem um prazo curto (por padrão, **2 horas**) para clicar no link e confirmar. Se não
   confirmar a tempo, a vaga passa para o próximo da fila e sua entrada fica como **Expirada**.

Na própria tela você acompanha o status de cada entrada: **Aguardando**, **Vaga oferecida —
confira seu e-mail**, **Confirmada**, **Expirada** ou **Cancelada**. Enquanto está "Aguardando",
o botão **Sair** remove você da fila.

> A vaga confirmada pela lista de espera já entra como uma consulta normal na sua lista de
> **Minhas Consultas**, seguindo o ciclo de dupla confirmação.

### 2.12 Avaliação pós-consulta (NPS)

Depois que o profissional registra o prontuário e a consulta é **concluída**, você recebe um
**e-mail convidando a avaliar o atendimento** (nota de 0 a 10 e um comentário opcional).

- O link é **público e de uso único** — não precisa estar logado para responder.
- O prazo para responder é de **7 dias**; depois disso o link expira.
- Responder é **opcional**; serve para a clínica medir a satisfação com o atendimento.

### 2.13 Perguntas frequentes (Paciente)

- **"O horário que eu queria sumiu da lista"** — outro paciente pode ter agendado esse mesmo
  horário primeiro, a hora já passou, ou o profissional retirou aquele horário da disponibilidade.
- **"Não achei nenhum horário com o profissional que eu quero"** — o profissional pode não ter
  publicado a agenda ainda. Entre na **Lista de Espera** ([2.11](#211-lista-de-espera)) para ser
  avisado quando abrir vaga.
- **"Não aparece nenhum profissional para eu escolher"** — a lista só mostra profissionais que já
  configuraram disponibilidade. Se está vazia, ainda não há agenda publicada.
- **"Não consigo confirmar minha consulta"** — confirme se o status já é "Aguardando paciente"; se
  ainda estiver "Agendada", o profissional ainda não confirmou a vez dele.
- **"Confirmei a minha parte e o status não virou 'Confirmada'"** — verifique se o profissional
  também já confirmou. A consulta só fica "Confirmada" com as **duas** confirmações.
- **"Não consigo agendar nada"** — verifique se sua conta está bloqueada por penalidade; a
  mensagem de erro informa a data e a hora em que o bloqueio termina.
- **"Onde eu cancelo minha consulta?"** — em **Minhas Consultas**, botão **Cancelar** no cartão da
  consulta (funciona em qualquer status ainda ativo). Ver [2.6](#26-cancelar-uma-consulta).
- **"Reagendei uma consulta que já estava confirmada e ela voltou para 'Agendada'"** — é o
  comportamento esperado: reagendar reinicia a dupla confirmação. Ver [2.7](#27-reagendar-uma-consulta).
- **"Cancelei uma consulta antiga e o sistema me avisou de bloqueio — fui bloqueado?"** — não. O
  aviso é preventivo; cancelamento de consulta cuja data já passou não aplica penalidade.
- **"Recebi um e-mail pedindo para avaliar a consulta — é do sistema?"** — sim, é a avaliação
  pós-consulta ([2.12](#212-avaliação-pós-consulta-nps)). O link não pede login e vale 7 dias.
- **"O médico disse que enviou um documento, mas não aparece em Meus Documentos"** — o documento
  só fica visível depois que o profissional o **libera**. Peça a ele para disponibilizar.

---

## 3. Manual do Profissional

Menu lateral ("Área do Profissional"): **Início · Minha Disponibilidade · Minha Agenda ·
Documentos**, com **Meu Perfil** no rodapé.

### 3.1 Meu Perfil

Além dos dados cadastrais, aqui você define sua **modalidade de atendimento**
(presencial / online / híbrido) — é essa informação que o paciente vê ao escolher entre
profissionais na hora de agendar.

### 3.2 Minha Disponibilidade

Grade de horários livres para atendimento, organizada por **data específica** (não por dia da
semana recorrente).

- Só é possível editar a grade do **mês atual ou do próximo** — meses passados não podem ser
  alterados.
- Cada horário marcado dura sempre **1 hora**.
- Se você tentar salvar uma grade removendo um horário que já tem uma consulta ativa vinculada
  (Aguardando Confirmação, Agendada, Aguardando paciente ou Confirmada), o sistema recusa e pede
  para cancelar aquela consulta individualmente primeiro.

Use as setas para navegar entre os meses, marque os horários livres na grade e clique em
**Salvar** para publicar.

> **Alerta "Atenção ao Prazo".** Se você ainda não publicou a agenda e faltam menos de 15 dias
> para o fim do mês, a tela mostra um aviso amarelo. Ele some assim que você salva pelo menos um
> horário no mês. É só um lembrete — não bloqueia nada.

### 3.3 Minha Agenda

Três abas organizam suas consultas, e **cada aba mostra botões diferentes**:

| Aba | O que lista | Botões disponíveis |
|---|---|---|
| **Hoje** | Atendimentos do dia. | Aprovar/Recusar (se "Aguardando Confirmação"); Confirmar (se "Agendada"); **Iniciar atendimento** e **Paciente faltou** (de "Agendada" em diante). |
| **Próximas** | Consultas futuras (não hoje). | Apenas **Aprovar/Recusar** ou **Confirmar**. Iniciar atendimento, marcar falta e cancelar **não** aparecem aqui — só no dia (aba Hoje) ou depois (aba Atrasadas). |
| **Atrasadas** | Consultas cuja data já passou e ainda sem desfecho (a agenda não "resolve" nada sozinha). | **Registrar atendimento**, **Paciente faltou** e **Cancelar** (ou só **Cancelar**, se ainda estiver "Aguardando Confirmação"). |

### 3.4 Aprovar ou recusar uma solicitação

Toda consulta nasce como uma solicitação do paciente, no status **Aguardando Confirmação**.
Nas abas Hoje/Próximas, use:

- **✓ Aprovar** — o status vira **Agendada**.
- **✕ Recusar** — pede uma justificativa obrigatória; o status vira **Cancelada**.

Depois que a consulta sair de "Aguardando Confirmação", os botões de aprovar/recusar somem — não
é mais possível aprovar/recusar de novo.

> **Recusar não tem desfazer.** A consulta vira "Cancelada" definitivamente; se foi engano, o
> paciente precisa solicitar o agendamento de novo.

### 3.5 Confirmar presença (sua parte na dupla confirmação)

Quando a consulta está **Agendada**, o botão **Confirmar** registra que você confirma o
atendimento. O status vira "Aguardando paciente" — falta agora a confirmação do lado dele.

### 3.5.1 Cancelar uma consulta que você já aceitou

Este é um ponto que costuma gerar dúvida. A tela **Minha Agenda** **não tem um botão "Cancelar"
para uma consulta futura já aceita** (status "Agendada", "Aguardando paciente" ou "Confirmada")
enquanto a data não chega. As opções do profissional são:

| Situação | O que fazer |
|---|---|
| A consulta ainda está **"Aguardando Confirmação"** | Use **✕ Recusar** ([3.4](#34-aprovar-ou-recusar-uma-solicitação)) — vira "Cancelada". |
| A consulta já foi aceita e a **data já passou** | Aba **Atrasadas** → botão **Cancelar** ([3.8](#38-cancelar-consulta-atrasada)). |
| A consulta já foi aceita e **ainda vai acontecer** | Não há botão na sua agenda. Peça à **administração** para cancelar (ela cancela qualquer consulta pela Agenda Geral — ver [4.4](#44-agenda-geral)). Alternativa: registrar o desfecho no dia (atendimento ou **Paciente faltou**). |

> Se esse fluxo (pedir à administração) for frequente na clínica, avalie com o time do sistema
> adicionar um botão **Cancelar** direto na agenda do profissional.

### 3.6 Iniciar/Registrar atendimento (prontuário)

O botão **Iniciar atendimento** (aba **Hoje**) ou **Registrar atendimento** (aba **Atrasadas**)
leva para a tela de **Prontuário Eletrônico** daquela consulta.

> **Atenção:** salvar o prontuário é o que **conclui a consulta** — não existe um botão separado
> de "concluir". Assim que você grava as notas clínicas, o status muda automaticamente para
> **Concluída**. Só é possível chegar a essa tela a partir de uma consulta que já existe; não há
> guarda de status prévio, então tecnicamente dá pra registrar o prontuário mesmo de uma consulta
> ainda não confirmada — mas o fluxo normal é registrar depois do atendimento de fato acontecer.

### 3.7 Marcar falta do paciente

Se o paciente não comparecer, use **Paciente faltou**. O status vira **Faltou** e uma penalidade é
aplicada à conta do paciente automaticamente, **sempre** — diferente do cancelamento, aqui não
existe regra de antecedência de 24h.

### 3.8 Cancelar consulta atrasada

Na aba **Atrasadas**, consultas ainda em "Aguardando Confirmação" só podem ser **canceladas**
(não faz mais sentido aprovar algo cuja data já passou). Para as demais, o botão **Cancelar**
também está disponível, junto de "Registrar atendimento" e "Paciente faltou".

### 3.9 Documentos — upload e visibilidade

Na tela **Documentos**:
1. Clique para abrir o formulário de novo documento.
2. Preencha título, tipo, e o conteúdo — texto direto ou upload de um **PDF**.
3. Marque **"Disponibilizar para o paciente imediatamente"** se quiser liberar na hora; senão, o
   documento fica oculto até você liberar depois.
4. Salve.

Depois de criado, o botão de disponibilidade (**Sim/Não** ou **Disponível/Oculto**) alterna a
visibilidade a qualquer momento — você decide quando o paciente passa a enxergar aquele laudo ou
atestado.

> PDFs são validados pela assinatura real do arquivo (não só pela extensão) e há um limite de
> tamanho de aproximadamente 5MB — arquivos disfarçados de PDF ou grandes demais são recusados.

### 3.10 Histórico clínico do paciente

A tela de **Prontuário Eletrônico** (aberta pelo botão de iniciar/registrar atendimento) mostra,
acima do campo de novas anotações, o **histórico de prontuários anteriores** daquele paciente —
consultas passadas com outros profissionais inclusive. É consulta apenas; você edita só a
anotação da consulta atual.

### 3.11 Início (Painel)

Mostra sua agenda do dia, pendências de confirmação, consultas atrasadas, suas próximas consultas
e o total de pacientes únicos que você já atendeu.

### 3.12 Exportar meus atendimentos

No painel **Início**, o botão **Exportar Meus Atendimentos** gera um **CSV** com os prontuários
que você criou — um backup do seu próprio trabalho clínico.

### 3.13 Perguntas frequentes (Profissional)

- **"Onde eu cancelo uma consulta que já aceitei?"** — enquanto a data não chega, não há botão na
  sua agenda; a administração cancela pela Agenda Geral. Ver [3.5.1](#351-cancelar-uma-consulta-que-você-já-aceitou).
- **"Na aba Próximas só aparece Confirmar / Aprovar / Recusar"** — é o esperado. Iniciar
  atendimento, marcar falta e cancelar só aparecem na aba **Hoje** (no dia) ou na aba
  **Atrasadas** (depois). Ver a tabela em [3.3](#33-minha-agenda).
- **"Recusei uma solicitação sem querer"** — não há como desfazer; a consulta fica "Cancelada".
  O paciente precisa solicitar de novo.
- **"Não consigo remover um horário da minha disponibilidade"** — provavelmente há uma consulta
  ativa vinculada a ele; cancele a consulta individualmente antes de tirá-lo da grade.
- **"Não consigo editar a disponibilidade de um mês"** — só o mês atual e o próximo podem ser
  editados.
- **"Confirmei a consulta e o status não ficou 'Confirmada'"** — falta a confirmação do paciente.
  Depois da sua confirmação o status fica "Aguardando paciente" até ele confirmar também.
- **"O prontuário concluiu a consulta antes da hora"** — é o comportamento esperado: salvar o
  prontuário sempre marca a consulta como Concluída, não existe um passo intermediário.
- **"Marquei falta e o paciente reclamou de bloqueio"** — falta **sempre** gera penalidade, sem
  janela de 24h. A 1ª é advertência; da 2ª em diante, bloqueio de 15 dias. Desbloqueio é com a
  administração.
- **"Meu PDF foi recusado no upload"** — o arquivo precisa ser um PDF de verdade (o sistema checa
  a assinatura do arquivo, não só a extensão `.pdf`) e ter até ~5MB.

---

## 4. Manual do Administrador

Menu lateral ("Administração"): **Início · Profissionais · Pacientes · Agenda Geral ·
Segurança · Logs**.

> Este capítulo vale igual para o perfil **Técnico** — ele vê o mesmo menu e tem o mesmo poder do
> Administrador (ver [4.9](#49-perfil-técnico)).

### 4.1 Início (Painel)

Visão geral da operação: totais de profissionais e pacientes cadastrados, e as consultas do dia
agrupadas por status. Os botões **Exportar Pacientes**, **Exportar Profissionais** e **Exportar
Relatório Geral** geram os respectivos arquivos **CSV** (ver [4.5](#45-exportações-administrativas)).

### 4.2 Profissionais — cadastrar, editar e excluir

- **+ Novo profissional** abre o formulário de cadastro (nome, e-mail, dados profissionais,
  modalidade de atendimento etc.). **Cadastrar** salva. É o único jeito de criar uma conta de
  profissional — profissionais não se auto-cadastram.
- **Editar** reabre o formulário preenchido; **Salvar alterações** grava.
- **Excluir** apaga o profissional. Se ele tiver consultas, prontuários, disponibilidade ou
  documentos vinculados, esta ação é uma **exclusão em cascata e definitiva** — o sistema mostra um
  alerta de confirmação antes ("Isso apagará DE FORMA PERMANENTE todas as consultas, prontuários,
  horários e documentos vinculados a ele. Essa ação não pode ser desfeita."). Leia o aviso com
  atenção antes de confirmar.

### 4.3 Pacientes — visualizar, desbloquear e excluir

A lista de pacientes pode ser filtrada por **Todos**, **Bloqueados** ou **Com infrações**.

- **Desbloquear** (aparece só em pacientes com bloqueio ativo) zera **todo** o histórico de
  penalidade daquele paciente de uma vez — bloqueio, contador de infrações e a advertência
  anterior. Não é um desbloqueio parcial.
- **Excluir** remove a conta. Se o paciente **não** tiver nenhuma consulta ou prontuário
  vinculado, o registro é apagado de fato. Se tiver, o sistema **anonimiza** em vez de apagar:
  nome, e-mail, CPF, telefone, endereço e demais dados de identificação são irreversivelmente
  sobrescritos e o login passa a ser impossível, mas o **prontuário clínico permanece intacto** —
  exigência legal de retenção por 20 anos (CFM), mesmo respeitando o direito ao esquecimento da
  LGPD.

### 4.4 Agenda Geral

Visão de todas as consultas do sistema, com busca por nome e filtros por status e por data. O
botão **Cancelar** fica disponível para consultas em **Agendada**, **Aguardando paciente** ou
**Confirmada** — o administrador pode cancelar qualquer consulta, de qualquer paciente ou
profissional, sempre com **justificativa obrigatória**, igual ao fluxo do paciente
(ver [2.6](#26-cancelar-uma-consulta)).

É por aqui que se cancela uma **consulta futura já aceita** quando o pedido parte do profissional
(a agenda dele não tem esse botão — ver [3.5.1](#351-cancelar-uma-consulta-que-você-já-aceitou)).

> Uma consulta ainda em **"Aguardando Confirmação"** não mostra o botão **Cancelar** na Agenda
> Geral — recusar uma solicitação pendente é ação do profissional (ou ela é cancelada pelo
> próprio paciente). Para consultas já atrasadas, o desfecho é dado pelo profissional na aba
> **Atrasadas** da agenda dele.

### 4.5 Exportações administrativas

| Botão | Formato | Conteúdo |
|---|---|---|
| Exportar Pacientes | CSV | Dados operacionais dos pacientes (CPF mascarado) |
| Exportar Profissionais | CSV | Dados operacionais dos profissionais |
| Exportar Relatório Geral | CSV | Consolidado de consultas |

### 4.6 Painel de Logs

Menu **Logs**. Lista automática de tudo que o sistema registrou como **aviso (WARN)** ou **erro
(ERROR)** em qualquer parte do backend — falha de envio de e-mail, erro de integração, exceção
inesperada. Serve para diagnosticar problemas sem depender de acesso ao servidor.

- Os registros ficam guardados por **30 dias** e depois são expurgados automaticamente.
- Dá para **filtrar por nível** (WARN / ERROR) e navegar por páginas.
- A gravação acontece em lote a cada poucos segundos, então um erro que acabou de ocorrer pode
  levar alguns segundos para aparecer.
- Uma mensagem de erro pode conter um dado pessoal citado incidentalmente (ex.: o e-mail de um
  paciente dentro do texto do erro). É a mesma informação que já apareceria no log do servidor;
  trate a tela com o mesmo cuidado.

### 4.7 Segurança (verificação em duas etapas do próprio administrador)

Menu **Segurança**. Como o Administrador e o Técnico não têm "Meu Perfil", esta tela existe para
eles ativarem/desativarem a **verificação em duas etapas** da própria conta. O passo a passo é o
mesmo da [1.6](#16-verificação-em-duas-etapas-2fa).

### 4.8 Perguntas frequentes (Administrador)

- **"O profissional pediu para cancelar uma consulta futura dele"** — faça pela **Agenda Geral**
  ([4.4](#44-agenda-geral)); a agenda do profissional não tem esse botão enquanto a data não chega.
- **"Não aparece o botão Cancelar numa consulta 'Aguardando Confirmação'"** — é esperado; essa
  solicitação é recusada pelo profissional ou cancelada pelo paciente.
- **"Excluí um profissional sem querer"** — a exclusão é permanente e em cascata; não há
  "lixeira". Confirme sempre com atenção o aviso antes de excluir.
- **"O paciente diz que está bloqueado mas já passaram os 15 dias"** — confira a data de
  `blockedUntil` nos detalhes do paciente; se o prazo já venceu, o próximo agendamento deveria
  funcionar normalmente. Use **Desbloquear** apenas para liberar antes do prazo.
- **"Desbloqueei o paciente mas queria manter a advertência"** — não dá: **Desbloquear** zera
  tudo de uma vez (bloqueio + contador de infrações + advertência).
- **"Excluí um paciente mas o prontuário ainda aparece em relatórios"** — é esperado: quando há
  vínculo clínico, a exclusão anonimiza a identidade, não apaga o prontuário.
- **"Preciso de uma conta de acesso técnico separada da administração da clínica"** — é o perfil
  **Técnico** ([4.9](#49-perfil-técnico)), criado por um administrador.

### 4.9 Perfil Técnico

O **Técnico** é uma conta com o **mesmo nível de acesso do Administrador** em todo o sistema — não
é um perfil restrito nem somente-leitura. Existe apenas para separar o acesso técnico/operacional
da administração real da clínica. Só um Administrador cria essa conta; não há auto-cadastro.

---

## 5. Máquina de estados da consulta — referência rápida

### 5.1 Diagrama de estados

```
Aguardando Confirmação ──profissional aprova──▶ Agendada ──profissional confirma──▶ Aguardando paciente ──paciente confirma──▶ Confirmada ──prontuário salvo──▶ Concluída
        │                                                                                                                          │
        └──profissional recusa──▶ Cancelada                        de qualquer estado: cancelar ▶ Cancelada · marcar falta ▶ Faltou · reagendar ▶ volta para Agendada
```

Pontos que costumam gerar dúvida:
- **Reagendar sempre reinicia a dupla confirmação** (a consulta volta para **Agendada**), mesmo
  partindo de uma consulta já Confirmada.
- **Só o registro do prontuário conclui a consulta** — não existe um botão "concluir" separado.
- **Falta sempre gera penalidade.** Cancelamento/reagendamento só gera penalidade quando as três
  condições valem juntas: faltam menos de 24h, a consulta **ainda está no futuro**, e ela já saiu
  de "Aguardando Confirmação". Cancelar consulta cuja data já passou **não** penaliza.

### 5.2 Cancelar, recusar e reagendar — quem faz o quê e onde

| Estado da consulta | Paciente | Profissional | Administrador / Técnico |
|---|---|---|---|
| **Aguardando Confirmação** (ainda não aceita) | **Cancelar** em *Minhas Consultas*. Sem penalidade. | **Recusar** em *Minha Agenda* (abas Hoje/Próximas; ou *Atrasadas* se a data passou) → vira Cancelada. | *Sem botão de cancelar na Agenda Geral neste estado.* |
| **Agendada / Aguardando paciente / Confirmada**, data ainda **no futuro** | **Cancelar** ou **Reagendar** em *Minhas Consultas*. Penalidade se < 24h. | **Sem botão** na agenda enquanto a data não chega. Encaminhar à administração. | **Cancelar** na *Agenda Geral* (justificativa obrigatória). |
| **Agendada / Aguardando paciente / Confirmada**, data **já passou** (atrasada) | **Cancelar** em *Minhas Consultas* (não penaliza — data passada). | Aba **Atrasadas**: **Registrar atendimento**, **Paciente faltou** ou **Cancelar**. | **Cancelar** na *Agenda Geral*. |
| **Concluída / Cancelada / Faltou** (encerrada) | — | — | — (estado final; não dá para reabrir) |

- **Reagendar** é exclusivo do **paciente** e só depois de a consulta ter sido aceita.
- **Marcar falta** é exclusivo do **profissional**, em qualquer data, e sempre aplica penalidade.
- Todo cancelamento (qualquer perfil) exige **justificativa**.

---

## 6. Perguntas frequentes gerais

### 6.1 Esqueci minha senha, e agora?
Veja [1.3](#13-esqueci-minha-senha). A mensagem de sucesso aparece sempre, mesmo que o e-mail
informado não exista no sistema — isso é proposital.

### 6.2 Por que recebi um erro "muitas tentativas, aguarde"?
Rotas sensíveis (login, exportações, prontuários, documentos, avaliação de consulta, lista de
espera) têm limite de 30 requisições por minuto por IP. Ao passar do limite, o sistema bloqueia
por 1 minuto — normalmente resolve sozinho sem precisar de suporte.

### 6.3 O sistema me desconectou sozinho
Depois de **15 minutos sem atividade** na área logada, a sessão expira por segurança e o sistema
volta para o login. Basta entrar de novo. Ver [1.7](#17-sessão-expirada-por-inatividade).

### 6.4 O login começou a pedir um código de 6 dígitos
Alguém ativou a **verificação em duas etapas** nessa conta ([1.6](#16-verificação-em-duas-etapas-2fa)).
O código vem do aplicativo autenticador. Sem o celular, use um **código de backup**. Sem o
aplicativo e sem os códigos de backup, a administração precisa ajudar a recuperar o acesso.

### 6.5 Não recebi um e-mail do sistema (verificação, redefinição de senha, vaga, avaliação)
Confira a caixa de spam/lixo eletrônico. Links têm validade: verificação de conta **24h**,
redefinição de senha **curta**, oferta de vaga da lista de espera **~2h**, avaliação de consulta
**7 dias**. Expirou? Para verificação e vaga, refaça a ação (login / entrar na fila) ou contate a
administração; para senha, peça um novo link em **Esqueci minha senha**.

### 6.6 Segurança da sessão
- Ao clicar em **Sair do sistema**, o acesso é revogado imediatamente no servidor, não só apagado
  do navegador.
- Dados sensíveis (CPF, telefone, endereço, alergias, conteúdo de prontuário e documentos) ficam
  **cifrados** no banco de dados.
- Toda leitura ou alteração de dado de saúde fica registrada em trilha de auditoria interna.

Para o detalhamento de conformidade com a LGPD (direitos do titular, retenção de dados, canal de
atendimento à privacidade), consulte o guia
[`documentacao/documentacao_cliente.html`](../documentacao/documentacao_cliente.html).
