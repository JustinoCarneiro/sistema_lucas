# Manual de Uso — Sistema Lucas

> Manual operacional, passo a passo, para os três perfis de acesso do sistema: **Paciente**,
> **Profissional** e **Administrador**. Descreve o que cada tela faz e como usá-la — não é a
> especificação técnica (isso está em [`docs/spec.md`](./spec.md)) nem o guia de conformidade
> LGPD voltado ao público (isso está em
> [`documentacao/documentacao_cliente.html`](../documentacao/documentacao_cliente.html), com foco
> em proteção de dados). Escrito a partir do comportamento real das telas em 03/08/2026.

## Sumário

1. [Acesso ao sistema (comum a todos os perfis)](#1-acesso-ao-sistema-comum-a-todos-os-perfis)
2. [Manual do Paciente](#2-manual-do-paciente)
3. [Manual do Profissional](#3-manual-do-profissional)
4. [Manual do Administrador](#4-manual-do-administrador)
5. [Máquina de estados da consulta — referência rápida](#5-máquina-de-estados-da-consulta--referência-rápida)
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
invalidado imediatamente no servidor (não basta fechar a aba — ver [Segurança](#63-segurança-da-sessão)).

### 1.5 Tema claro/escuro

O ícone de sol/lua no topo da tela alterna entre tema claro e escuro. A preferência fica salva
para os próximos acessos.

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

Qualquer consulta pode ser cancelada por você, em qualquer status. Ao cancelar:
- é obrigatório escrever uma **justificativa** (mínimo de 10 caracteres);
- se faltar **menos de 24h** para o horário marcado **e** a consulta já tiver passado da fase
  "Aguardando Confirmação", uma penalidade é aplicada à sua conta (ver [2.9](#29-penalidades-por-falta-ou-cancelamento-tardio));
- cancelamentos com 24h ou mais de antecedência, ou de consultas ainda não aceitas pelo
  profissional, não geram penalidade.

### 2.7 Reagendar uma consulta

Ao reagendar para nova data/horário, a consulta **volta para "Aguardando Confirmação"** e todo o
ciclo de dupla confirmação recomeça — mesmo que ela já estivesse totalmente confirmada antes.

### 2.8 Meus Documentos

Nesta tela você vê os documentos clínicos (laudos, atestados, exames) que o profissional
**liberou** para você. Documentos ainda não liberados pelo profissional não aparecem. Documentos
em PDF podem ser baixados pelo botão **Baixar PDF**.

### 2.9 Penalidades por falta ou cancelamento tardio

O sistema aplica penalidade progressiva quando você falta a uma consulta ou cancela com menos de
24h de antecedência (depois que a consulta já havia sido aceita pelo profissional):

1. **1ª ocorrência:** advertência por e-mail. Sua conta **não** é bloqueada.
2. **2ª ocorrência:** sua conta fica **bloqueada por 15 dias** para novos agendamentos, com aviso
   por e-mail.

Uma falta marcada pelo profissional (você não compareceu) **sempre** gera penalidade, mesmo que a
data já estivesse próxima ou distante — não há regra de antecedência para falta, só para
cancelamento.

Se precisar de desbloqueio antes do prazo, contate a administração (ver [4.3](#43-pacientes--visualizar-desbloquear-e-excluir)).

### 2.10 Portabilidade — exportar meus dados

Na tela **Meus Documentos**, o botão **Portabilidade (Exportar Meus Dados)** gera um arquivo
**JSON** com todos os seus dados: cadastro, prontuários, documentos, consultas e o registro do seu
consentimento aos Termos de Uso — seu direito de portabilidade garantido pela LGPD (Art. 18, V).

### 2.11 Perguntas frequentes (Paciente)

- **"O horário que eu queria sumiu da lista"** — outro paciente pode ter agendado esse mesmo
  horário primeiro, ou a hora já passou.
- **"Não consigo confirmar minha consulta"** — confirme se o status já é "Aguardando paciente"; se
  ainda estiver "Agendada", o profissional ainda não confirmou a vez dele.
- **"Não consigo agendar nada"** — verifique se sua conta está bloqueada por penalidade; a
  mensagem de erro informa até quando.

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

### 3.3 Minha Agenda

Três abas organizam suas consultas:

- **Hoje** — atendimentos do dia.
- **Próximas** — consultas futuras.
- **Atrasadas** — consultas cuja data já passou e que ainda precisam de uma ação sua (a agenda não
  "resolve" nada sozinha).

### 3.4 Aprovar ou recusar uma solicitação

Toda consulta nasce como uma solicitação do paciente, no status **Aguardando Confirmação**.
Nas abas Hoje/Próximas, use:

- **✓ Aprovar** — o status vira **Agendada**.
- **✕ Recusar** — pede uma justificativa obrigatória; o status vira **Cancelada**.

Depois que a consulta sair de "Aguardando Confirmação", os botões de aprovar/recusar somem — não
é mais possível aprovar/recusar de novo.

### 3.5 Confirmar presença (sua parte na dupla confirmação)

Quando a consulta está **Agendada**, o botão **Confirmar** registra que você confirma o
atendimento. O status vira "Aguardando paciente" — falta agora a confirmação do lado dele.

### 3.6 Iniciar/Registrar atendimento (prontuário)

O botão **Iniciar atendimento** (nas abas Hoje/Próximas) ou **Registrar atendimento** (na aba
Atrasadas) leva para a tela de **Prontuário Eletrônico** daquela consulta.

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

### 3.10 Início (Painel)

Mostra sua agenda do dia, pendências de confirmação, consultas atrasadas, suas próximas consultas
e o total de pacientes únicos que você já atendeu.

### 3.11 Exportar meus atendimentos

No painel **Início**, o botão **Exportar Meus Atendimentos** gera um **CSV** com os prontuários
que você criou — um backup do seu próprio trabalho clínico.

### 3.12 Perguntas frequentes (Profissional)

- **"Não consigo remover um horário da minha disponibilidade"** — provavelmente há uma consulta
  ativa vinculada a ele; cancele a consulta individualmente antes de tirá-lo da grade.
- **"Não consigo editar a disponibilidade de um mês"** — só o mês atual e o próximo podem ser
  editados.
- **"O prontuário concluiu a consulta antes da hora"** — é o comportamento esperado: salvar o
  prontuário sempre marca a consulta como Concluída, não existe um passo intermediário.

---

## 4. Manual do Administrador

Menu lateral ("Administração"): **Início · Profissionais · Pacientes · Agenda Geral**.

### 4.1 Início (Painel)

Visão geral da operação: totais de profissionais e pacientes cadastrados, e as consultas do dia
agrupadas por status. Os botões **Exportar Pacientes**, **Exportar Profissionais** e **Exportar
Relatório Geral** geram os respectivos arquivos **CSV** (ver [4.5](#45-exportações-administrativas)).

### 4.2 Profissionais — cadastrar, editar e excluir

- **+ Novo profissional** abre o formulário de cadastro (nome, e-mail, dados profissionais,
  modalidade de atendimento etc.). **Cadastrar** salva.
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

Visão de todas as consultas do sistema, com filtro por status. O botão **Cancelar** fica
disponível para consultas em **Agendada**, **Aguardando paciente** ou **Confirmada** — o
administrador pode cancelar qualquer consulta, de qualquer paciente ou profissional, sempre com
justificativa obrigatória, igual ao fluxo do paciente (ver [2.6](#26-cancelar-uma-consulta)).

### 4.5 Exportações administrativas

| Botão | Formato | Conteúdo |
|---|---|---|
| Exportar Pacientes | CSV | Dados operacionais dos pacientes (CPF mascarado) |
| Exportar Profissionais | CSV | Dados operacionais dos profissionais |
| Exportar Relatório Geral | CSV | Consolidado de consultas |

### 4.6 Perguntas frequentes (Administrador)

- **"Excluí um profissional sem querer"** — a exclusão é permanente e em cascata; não há
  "lixeira". Confirme sempre com atenção o aviso antes de excluir.
- **"O paciente diz que está bloqueado mas já passaram os 15 dias"** — confira a data de
  `blockedUntil` nos detalhes do paciente; se o prazo já venceu, o próximo agendamento deveria
  funcionar normalmente. Use **Desbloquear** apenas para liberar antes do prazo.
- **"Excluí um paciente mas o prontuário ainda aparece em relatórios"** — é esperado: quando há
  vínculo clínico, a exclusão anonimiza a identidade, não apaga o prontuário.

---

## 5. Máquina de estados da consulta — referência rápida

```
Aguardando Confirmação ──profissional aprova──▶ Agendada ──profissional confirma──▶ Aguardando paciente ──paciente confirma──▶ Confirmada ──prontuário salvo──▶ Concluída
        │                                                                                                                          │
        └──profissional recusa──▶ Cancelada                        de qualquer estado: cancelar ▶ Cancelada · marcar falta ▶ Faltou · reagendar ▶ volta para Agendada
```

Pontos que costumam gerar dúvida:
- **Reagendar sempre reinicia a dupla confirmação**, mesmo partindo de uma consulta já Confirmada.
- **Só o registro do prontuário conclui a consulta** — não existe um botão "concluir" separado.
- **Falta sempre gera penalidade**; cancelamento só gera penalidade se for a menos de 24h e a
  consulta já tiver saído de "Aguardando Confirmação".

---

## 6. Perguntas frequentes gerais

### 6.1 Esqueci minha senha, e agora?
Veja [1.3](#13-esqueci-minha-senha). A mensagem de sucesso aparece sempre, mesmo que o e-mail
informado não exista no sistema — isso é proposital.

### 6.2 Por que recebi um erro "muitas tentativas, aguarde"?
Rotas sensíveis (login, exportações, prontuários, documentos) têm limite de 30 requisições por
minuto por IP. Ao passar do limite, o sistema bloqueia por 1 minuto — normalmente resolve sozinho
sem precisar de suporte.

### 6.3 Segurança da sessão
- Ao clicar em **Sair do sistema**, o acesso é revogado imediatamente no servidor, não só apagado
  do navegador.
- Dados sensíveis (CPF, telefone, endereço, alergias, conteúdo de prontuário e documentos) ficam
  **cifrados** no banco de dados.
- Toda leitura ou alteração de dado de saúde fica registrada em trilha de auditoria interna.

Para o detalhamento de conformidade com a LGPD (direitos do titular, retenção de dados, canal de
atendimento à privacidade), consulte o guia
[`documentacao/documentacao_cliente.html`](../documentacao/documentacao_cliente.html).
