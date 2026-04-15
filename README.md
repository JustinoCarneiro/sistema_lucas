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

### 3.2 Proteção de Dados (Data-at-Rest)
*   **Criptografia AES**: Dados sensíveis (como `notas` de prontuário e arquivos de documentos) são criptografados antes de serem salvos no banco.
    *   **Algoritmo**: AES-128 (configurável para AES-256).
    *   **Implementação**: Via [EncryptionConverter.java](file:///home/marcos/Applications/sistema_lucas/backend/src/main/java/com/sistema/lucas/config/jpa/EncryptionConverter.java), garantindo criptografia transparente no nível de persistência.

### 3.3 Verificação de Identidade
*   **Fluxo de E-mail**: Novas contas são criadas com o status `verified = false`. O sistema envia automaticamente um e-mail com um token único de verificação (expira em 24h).
*   **Banner Informativo**: O frontend exibe um aviso visual para usuários com e-mail ainda não validado.

### 3.4 Rastreabilidade e Auditoria
*   **Audit Log**: Todas as visualizações e exportações de dados sensíveis são registradas em uma tabela de auditoria dedicada, capturando:
    *   ID do Usuário executor.
    *   Entidade acessada.
    *   Timestamp da operação.

---

## 🆕 4. Novas Funcionalidades
*   **Gestão de Disponibilidade Profissional**: Interface intuitiva para profissionais configurarem seus dias e horários de trabalho semanais, com geração automática de slots de 1h.
*   **Fluxo de Agendamento Guiado**: Pacientes visualizam apenas profissionais com disponibilidade configurada, escolhendo data e hora em tempo real.
*   **Ciclo de Confirmação Invertido**: Segurança reforçada onde o agendamento passa pelos estados:
    1.  `AGENDADA` (Aguardando profissional)
    2.  `CONFIRMADA_PROFISSIONAL` (Aguardando paciente)
    3.  `CONFIRMADA` (Presença confirmada por ambas as partes)
*   **Registro de Pacientes**: Formulário completo com máscaras (CPF, WhatsApp) e validação em tempo real.
*   **Verificação de E-mail**: Integração com Mailtrap/SMTP para validação segura de novas contas.
*   **Notificações In-page**: Feedback de sucesso e erro via Signals, eliminando alertas pop-up intrusivos.
*   **Roteamento SPA Seguro**: Configuração Nginx customizada com `try_files` para garantir funcionamento perfeito das rotas Angular em ambiente Docker.
*   **Blindagem de Concorrência**: `UNIQUE CONSTRAINT` no banco de dados para a dupla `(professional_id, date_time)`, garantindo imunidade técnica total contra duplicação de horários (*race conditions*).

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

## ✅ 6. Qualidade e Testes
O sistema conta com uma suíte robusta de testes automatizados:
*   **Cypress (E2E)**: Localizado em `frontend/cypress/e2e/`. 
    *   **11-disponibilidade**: Valida toda a configuração de grade horária do profissional.
    *   **12-agendamento-confirmacao**: Testa o ciclo completo de reserva e o fluxo de confirmações mútua.
    *   **Autenticação**: Cobre registro, login e proteção de rotas.
*   **JUnit (Integração)**: Localizado em `backend/src/test/`.
    *   Cobre endpoints de autenticação, lógica de disponibilidade e criptografia AES.

---

> [!TIP]
> Para rodar os testes E2E localmente, certifique-se de que o container está rodando e execute `npm run cypress:open` ou `npm run cypress:run` na pasta `frontend`.

> [!IMPORTANT]
> Em produção, certifique-se de configurar o **Nginx** como proxy reverso para lidar com SSL (HTTPS) e redirecionamento de portas.
