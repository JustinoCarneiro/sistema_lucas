# Documentação Técnica: Sistema Lucas

Esta documentação fornece uma visão detalhada da arquitetura, camadas de segurança e diretrizes de escalabilidade do Sistema Lucas (Prontuário Eletrônico e Gestão Clínica).

## 1. Visão Geral do Sistema
O Sistema Lucas é uma plataforma web para gestão de clínicas e consultórios, focada em segurança de dados sensíveis e conformidade com a LGPD. Atende três perfis de usuário: **Admin**, **Profissional de Saúde** e **Paciente**.

## 2. Arquitetura de Alto Nível
O sistema segue o padrão **Monolito Modular** com separação clara entre Frontend e Backend.

- **Frontend**: Single Page Application (SPA) desenvolvida em Angular.
- **Backend**: API RESTful desenvolvida com Java/Spring Boot.
- **Banco de Dados**: PostgreSQL para persistência relacional.
- **Infraestrutura**: Containerização com Docker Compose.

## 3. Backend (Spring Boot)
Organizado em camadas para garantir a separação de responsabilidades (SoC):

### Camadas:
- **`model`**: Entidades JPA que representam o esquema do banco de dados.
- **`repository`**: Interfaces que utilizam Spring Data JPA para acesso ao banco.
- **`service`**: Camada de lógica de negócio, onde residem as regras de validação e processamento.
- **`controller`**: Endpoints REST que gerenciam as requisições HTTP.
- **`config`**: Configurações do Spring (Security, CORS, Data Initializer).
- **[security](file:///home/marcos/Applications/sistema_lucas/backend/src/main/java/com/sistema/lucas/security/config/SecurityConfigurations.java#30-48)**: Implementação de JWT, filtros de autenticação e RBAC.

### Segurança e Privacidade:
- **Criptografia AES-256**: Aplicada via [EncryptionConverter](file:///home/marcos/Applications/sistema_lucas/backend/src/main/java/com/sistema/lucas/config/jpa/EncryptionConverter.java#12-52) em campos sensíveis (`notas` de prontuário e arquivos de documentos) diretamente no nível de persistência.
- **Log de Auditoria**: Sistema que rastreia `visualizações` e `exportações` de dados, registrando usuário, timestamp e entidade afetada.
- **RBAC (Role-Based Access Control)**: Controle rigoroso de acesso baseado em Roles (`ADMIN`, `PROFESSIONAL`, `PATIENT`).

## 4. Frontend (Angular)
Arquitetura baseada em componentes reativos e design responsivo.

- **Framework**: Angular 18+.
- **Estilização**: Tailwind CSS (utilitários para UI premium e responsiva).
- **Gerenciamento de Estado**: Signals para reatividade performática.
- **Roteamento**: Guards para proteção de rotas privadas.

## 5. Modelo de Dados (ER)
As principais entidades são:
- [User](file:///home/marcos/Applications/sistema_lucas/backend/src/main/java/com/sistema/lucas/model/User.java#16-102): Base para autenticação.
- [Professional](file:///home/marcos/Applications/sistema_lucas/backend/src/main/java/com/sistema/lucas/model/Professional.java#9-38) / [Patient](file:///home/marcos/Applications/sistema_lucas/backend/src/main/java/com/sistema/lucas/model/Patient.java#11-58): Especializações de [User](file:///home/marcos/Applications/sistema_lucas/backend/src/main/java/com/sistema/lucas/model/User.java#16-102).
- [Appointment](file:///home/marcos/Applications/sistema_lucas/backend/src/main/java/com/sistema/lucas/model/Appointment.java#10-49): Gestão de horários.
- [Prontuario](file:///home/marcos/Applications/sistema_lucas/backend/src/main/java/com/sistema/lucas/model/Prontuario.java#9-41): Registro clínico (criptografado).
- [Documento](file:///home/marcos/Applications/sistema_lucas/backend/src/main/java/com/sistema/lucas/model/Documento.java#10-55): Gestão de laudos e exames (criptografado).
- [AuditLog](file:///home/marcos/Applications/sistema_lucas/backend/src/main/java/com/sistema/lucas/model/AuditLog.java#7-31): Rastro de segurança.

## 6. Guia de Escalabilidade e Manutenibilidade

### Manutenibilidade:
1. **Testes Automatizados**: Manter a cobertura com Cypress (E2E) para fluxos críticos.
2. **Documentação Interna**: Utilizar Javadoc para métodos complexos na camada de `service`.
3. **Padrão de Código**: Seguir as convenções de nomes e estrutura de pacotes já estabelecidas no projeto.

### Escalabilidade:
1. **Banco de Dados**: Migrar para instâncias gerenciadas (ex: RDS) e utilizar réplicas de leitura se o volume de exportações crescer.
2. **Stateless API**: Como usamos JWT, o backend pode ser escalado horizontalmente por trás de um Load Balancer.
3. **Micro-serviços**: Se o módulo de `Exportação` ou [Documentos](file:///home/marcos/Applications/sistema_lucas/frontend/src/app/pages/document-management/document-management.ts#74-81) (OCR/Processamento) tornar-se gargalo, eles estão prontos para serem extraídos como serviços independentes.
