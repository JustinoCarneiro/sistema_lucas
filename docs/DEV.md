# Rodando o Sistema Lucas localmente

## Ambiente local
```bash
./deploy-dev.sh      # Sobe todos os containers usando .env.dev (semeia dados fake via DataInitializer)
./deploy-prod.sh     # Sobe containers usando .env (banco limpo, config de produção)
./push-and-deploy.sh # rsync + SSH pro servidor de produção remoto
```
Frontend em `http://localhost:8082`, backend em `http://localhost:8081`.

## Backend (Maven, a partir de `backend/`)
```bash
./mvnw spring-boot:run          # Roda o backend localmente (precisa do banco)
./mvnw test                     # Roda todos os testes JUnit
./mvnw test -Dtest=NomeDaClasse # Roda uma única classe de teste
./mvnw package -DskipTests      # Build do JAR sem rodar testes
```

## Frontend (a partir de `frontend/`)
```bash
npm start                        # ng serve (dev server na porta 4200)
npm run build                    # ng build (build de produção)
npm test                         # testes unitários vitest
npm run cypress:open             # Cypress E2E interativo
npm run cypress:run              # Cypress E2E headless (precisa dos containers rodando)
```

## Arquitetura

### Camadas do backend
Arquitetura em camadas estrita: **Controller → Service → Repository → Entity**
- `controller/` — endpoints REST; nunca retorna `@Entity` diretamente.
- `service/` — lógica de negócio; todo método público é fronteira de transação.
- `repository/` — interfaces Spring Data JPA.
- `model/` — entidades JPA, DTOs (Java Records), enums.
- `security/` — filtro JWT, rate limiting, exception handler global.
- `config/` — inicializadores, conversores JPA.

### Estrutura do frontend
Angular 21 com standalone components e Signals.
- `app/pages/` — uma pasta por página, cada uma com `.ts`, `.html` e geralmente um `.service.ts`.
- `app/security/` — `auth.service.ts`, `auth.guard.ts`, `auth.interceptor.ts` (injeta o JWT em toda requisição HTTP).
- `app/app.routes.ts` — todas as definições de rota.
- `app/app.config.ts` — `provideHttpClient` com `authInterceptor`.

### Migrations de banco
Flyway gerencia o schema. Arquivos em `backend/src/main/resources/db/migration/`, rodam automaticamente no startup. Nunca modificar uma migration já existente — sempre criar uma `V{n}__descricao.sql` nova.

### Configuração de ambiente
Copiar `.env.dev` pro dev local. Variáveis-chave:
- `SPRING_PROFILES_ACTIVE` — `dev` semeia dado fake (DataInitializer), `prod` inicia limpo.
- `ENCRYPTION_KEY` — chave AES-256 (32 bytes); `api.security.encryption.legacy-keys` (CSV) guarda chaves antigas pra migração/rotação (ver `EncryptionConverter`).
- `JWT_SECRET` — string aleatória de 32+ caracteres.
- `CPF_HASH_PEPPER` — pepper do HMAC-SHA256 usado no hash de CPF (`CpfHashService`).
- `INITIAL_ADMIN_EMAIL`/`INITIAL_ADMIN_PASSWORD` — credenciais do primeiro admin (criado por `AdminInitializer` se o banco estiver vazio).
- `ALLOWED_ORIGINS` — origens CORS separadas por vírgula.

> ⚠️ Ver alerta de segurança na raiz da memória técnica: o `.env.dev` do repositório já foi commitado com valores reais no histórico do git — sempre tratar os valores atuais desse arquivo como comprometidos até rotação confirmada.

## Notas de teste
- Backend: testes JUnit em `backend/src/test/java/com/sistema/lucas/` (18 arquivos, ~118 `@Test`).
- Frontend unitário: vitest (`*.spec.ts`, 24 arquivos, ~189 blocos `it`).
- E2E: Cypress em `frontend/cypress/e2e/` — **20 suítes** cobrindo a jornada completa (nota: `README.md` e o `CLAUDE.md` anterior diziam "13 suítes", desatualizado — a contagem real no disco é 20).
- Cypress precisa dos containers rodando (`./deploy-dev.sh` primeiro).
- Suíte adicional gerada por TestSprite (ferramenta de QA via IA) em `testsprite_tests/` — 30 scripts Python, stack de teste à parte do JUnit/Vitest/Cypress.
