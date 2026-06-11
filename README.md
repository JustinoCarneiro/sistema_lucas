# Documentação Técnica: Projeto Lucas

Esta documentação fornece uma visão detalhada da arquitetura, camadas de segurança e diretrizes de infraestrutura do Projeto Lucas (Prontuário Eletrônico e Gestão Clínica).

---

## 🚀 1. Visão Geral do Sistema
O Projeto Lucas é uma plataforma web para gestão de clínicas e consultórios, com foco em segurança de dados sensíveis e conformidade com a LGPD. 

### Perfis de Usuário:
*   **Admin**: Gestão total de usuários, profissionais e logs.
*   **Profissional**: Gestão de agenda, prontuários e documentos dos pacientes.
*   **Paciente**: Consulta de agendamentos e verificação de conta.

---

## 🛠️ 2. Arquitetura e Tecnologias
O sistema utiliza uma arquitetura de **Monolito Modular** containerizado, garantindo isolamento entre os serviços.

*   **Frontend**: Angular 18+ com Tailwind CSS e Signals (reatividade performática).
*   **Backend**: Spring Boot 3.4+ (Java 21) seguindo padrões RESTful.
*   **Banco de Dados**: PostgreSQL 15 (Persistência Relacional).
*   **Infraestrutura**: Docker Compose para orquestração de containers.

---

## 🔐 3. Segurança e Privacidade (Foco LGPD)
A segurança é o pilar central do Sistema Lucas, implementada em múltiplas camadas:

### 3.1 Proteção de Acesso
*   **Autenticação**: Stateless via **JWT (JSON Web Token)** com expiração configurável.
*   **Hashing de Senhas**: Utilização do algoritmo **Argon2id** (vencedor do Password Hashing Competition), oferecendo proteção superior contra ataques de força bruta e dicionário.
*   **RBAC (Role-Based Access Control)**: Controle de acesso granular baseado em permissões (@PreAuthorize) no nível de endpoint.
*   **Gestão de Sessão**: Rate Limiting (Bucket4j) e expiração por inatividade para evitar sequestro de sessão.

### 3.2 Proteção de Dados e Privacidade (LGPD)
*   **Criptografia AES-128 GCM**: Dados sensíveis textuais (como `notas` de prontuário e títulos de documentos) são criptografados antes de serem salvos no banco de dados via `EncryptionConverter`.
*   **Proteção de PII (CPF)**: O CPF dos usuários é armazenado via **Hashing unidirecional (HMAC-SHA256)**, garantindo a unicidade do registro sem expor o dado real em caso de vazamento.
*   **Direito ao Esquecimento (Anonimização)**: Ao solicitar exclusão da conta, pacientes com vínculos clínicos ativos (consultas/prontuários) sofrem **anonimização irreversível** em vez de deleção física, preservando o histórico médico exigido pelo CFM enquanto atende à LGPD.

### 3.3 Verificação de Identidade
*   **Fluxo de E-mail**: Novas contas são criadas com o status `verified = false`. O sistema envia automaticamente um e-mail com um token único de verificação (expira em 24h).
*   **Recuperação de Senha**: Fluxo seguro via e-mail com token temporário para redefinição de credenciais.

### 3.4 Rastreabilidade e Auditoria
*   **Audit Log**: Todas as visualizações e exportações de dados sensíveis são registradas em uma tabela de auditoria dedicada, capturando:
    *   ID do Usuário executor.
    *   Entidade acessada.
    *   Timestamp da operação.

---

## 🆕 4. Funcionalidades Principais
*   **Gestão de Disponibilidade Profissional**: Interface intuitiva para profissionais configurarem seus dias e horários de trabalho mensais (Mês Atual e Próximo Mês), com geração automática de slots.
*   **Fluxo de Agendamento Guiado**: Pacientes visualizam apenas profissionais com disponibilidade configurada.
*   **Tratamento de Concorrência (Anti-Overbooking)**: Trava de banco de dados (*Partial Unique Index*) que bloqueia duplo agendamento no mesmo horário, liberando a vaga automaticamente caso o primeiro agendamento seja cancelado ou recusado.
*   **Ciclo de Confirmação Invertido**: Segurança reforçada onde o agendamento passa por: `AGENDADA` (Aguardando profissional) → `CONFIRMADA_PROFISSIONAL` (Aguardando paciente) → `CONFIRMADA`.
*   **Governança de Cancelamentos**: Exigência sistêmica de justificativa textual sempre que um agendamento for recusado ou cancelado (aplicável tanto a profissionais quanto a pacientes).
*   **Sistema de Penalidades (Anti-Absenteísmo)**: Cancelamentos com menos de 24h ou ausências (faltas) geram infrações automáticas. A 2ª infração resulta em um **bloqueio de 15 dias** para novos agendamentos.
*   **Alertas de Consultas Atrasadas**: Notificações por e-mail e banners urgentes no dashboard do profissional caso existam consultas com data passada pendentes de atualização de status.
*   **Gestão Administrativa**: A tela de 'Agenda Geral' (Admin) oferece filtros em tempo real por texto (busca), data exata e status da consulta.
*   **Prontuário Eletrônico**: Evolução clínica cronológica com garantia de acesso exclusivo ao médico vinculado (IDOR protection).
*   **Gestão de Documentos**: Upload e download seguros de exames e laudos, visíveis para o paciente.
*   **Exportação de Dados**: Relatórios em CSV e PDF (Admin) com registro em Audit Log.

---

## 🐳 5. Infraestrutura e Deploy
O projeto utiliza Docker Compose e perfis automáticos Spring (`dev` e `prod`) para separar completamente os dados e comportamentos de infraestrutura.

### Scripts de Deploy:
*   **`deploy-dev.sh`**: Sobe o ambiente local carregando o perfil de desenvolvimento e populando o banco (`DataInitializer`) com dados falsos. Lê do `.env.dev`.
*   **`deploy-prod.sh`**: Sobe o ambiente de produção isolado e com credenciais de produção lidas do `.env` principal. Banco de dados sobe limpo.
*   **`push-and-deploy.sh`**: Script para desenvolvedores. Envia as alterações via `rsync` ignorando arquivos indesejados (`.geminiignore`, etc.) e dispara o deploy remoto.

### Inicialização Segura (O "Primeiro Admin")
Na produção, a base nasce vazia. Para evitar credenciais no código-fonte, o sistema possui um `AdminInitializer` que checa se o sistema é novo e **cria automaticamente a primeira conta de Administrador** usando o e-mail e senha definidos nas suas variáveis do `.env` (`INITIAL_ADMIN_EMAIL` e `INITIAL_ADMIN_PASSWORD`).

### Como rodar localmente (Desenvolvimento):
1.  Verifique se o arquivo `.env.dev` está configurado.
2.  Execute `./deploy-dev.sh`.
3.  O frontend estará disponível em `http://localhost:8082` e o backend em `http://localhost:8081`. O sistema já nascerá com usuários falsos para testes (Dr. Carlos, Dra. Ana, etc.).

---

## ✅ 6. Qualidade e Testes (Roadmap)
O sistema possui uma estratégia de testes documentada e em franca expansão (veja `roadmap_testes.md`), visando cobrir ~435 cenários de negócio críticos:

*   **Cypress (E2E Frontend)**: Localizado em `frontend/cypress/e2e/`. 
    *   Possui **13 suítes ativas** cobrindo E2E de Autenticação, Agendamentos, Disponibilidade, Penalidades e Painel Admin.
    *   Suítes programadas: Recuperação de Senha, Verificação de E-mail, Privacidade e Consultas Atrasadas.
*   **Vitest (Unitários Frontend)**: Localizado junto aos componentes Angular em `frontend/src/app/pages/`.
*   **JUnit 5 + Mockito (Unitários/Integração Backend)**: Localizado em `backend/src/test/java/`.
    *   Cobre `AppointmentService`, LGPD/Anonimização, Criptografia AES, Autenticação e Controllers via `@WebMvcTest`.

---

> [!TIP]
> Para rodar os testes E2E localmente, certifique-se de que o container está rodando e execute `npm run cypress:open` ou `npm run cypress:run` na pasta `frontend`.

> [!IMPORTANT]
> Em produção, certifique-se de configurar o **Nginx** como proxy reverso para lidar com SSL (HTTPS) e redirecionamento de portas.
