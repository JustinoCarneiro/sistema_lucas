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
*   **Registro de Pacientes**: Formulário completo com máscaras (CPF, WhatsApp) e validação em tempo real.
*   **Verificação de E-mail**: Integração com Mailtrap/SMTP para validação segura de novas contas.
*   **Notificações In-page**: Feedback de sucesso e erro via Signals, eliminando alertas pop-up intrusivos.
*   **Roteamento SPA Seguro**: Configuração Nginx customizada com `try_files` para garantir funcionamento perfeito das rotas Angular em ambiente Docker.

---

## 🐳 5. Infraestrutura e Deploy
O projeto está pronto para produção com suporte a builds internos seguros.

### Scripts de Deploy:
*   **`deploy.sh`**: Realiza o build **dentro do Docker** (multi-stage). Compila o Java e o Angular em ambientes isolados e atualiza os containers.
*   **`push-and-deploy.sh`**: Script para desenvolvedores. Envia as alterações via `rsync` para o servidor e dispara o `deploy.sh` via SSH.

### Como rodar localmente:
1.  Configure o arquivo `.env` com suas credenciais.
2.  Execute `docker compose up -d --build`.

---

## ✅ 6. Qualidade e Testes
O sistema conta com uma suite de testes automatizados de ponta a ponta:
*   **Cypress (E2E)**: Localizado em `frontend/cypress/e2e/`. 
    *   Cobre registro de sucesso, validações de erro, exibição de banner e fluxo de callback de verificação.
*   **JUnit (Integração)**: Localizado em `backend/src/test/`.
    *   Cobre endpoints de autenticação e lógica de tokens.

---

> [!TIP]
> Em produção, certifique-se de configurar o **Nginx** como proxy reverso para lidar com SSL (HTTPS) e redirecionamento de portas.
