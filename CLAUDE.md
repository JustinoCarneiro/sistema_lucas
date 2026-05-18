# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Sistema Lucas is a full-stack clinical management platform (prontuário eletrônico) with strong LGPD compliance focus. It uses a containerized modular monolith architecture: Spring Boot 3.4 (Java 21) backend, Angular 21 frontend, PostgreSQL 15 database — all orchestrated via Docker Compose.

## Development Commands

### Local Dev Environment
```bash
./deploy-dev.sh      # Start all containers using .env.dev (seeds fake data via DataInitializer)
./deploy-prod.sh     # Start containers using .env (clean DB, production config)
./push-and-deploy.sh # rsync + SSH deploy to remote production server
```
Frontend runs on `http://localhost:8082`, backend on `http://localhost:8081`.

### Backend (Maven, from `backend/`)
```bash
./mvnw spring-boot:run          # Run backend locally (requires DB)
./mvnw test                     # Run all JUnit tests
./mvnw test -Dtest=ClassName    # Run a single test class
./mvnw package -DskipTests      # Build JAR without tests
```

### Frontend (from `frontend/`)
```bash
npm start                        # ng serve (dev server on port 4200)
npm run build                    # ng build (production build)
npm test                         # vitest unit tests
npm run cypress:open             # Cypress E2E interactive mode
npm run cypress:run              # Cypress E2E headless (requires containers running)
```

## Architecture

### Backend Layers
Strict layered architecture: **Controller → Service → Repository → Entity**

- `controller/` — REST endpoints; never return `@Entity` classes directly
- `service/` — business logic; all public methods are transaction boundaries
- `repository/` — Spring Data JPA interfaces
- `model/` — JPA entities, DTOs (Java Records), enums
- `security/` — JWT filter, rate limiting, global exception handler
- `config/` — initializers, JPA converters

### Frontend Structure
Angular 21 standalone components with Signals for reactivity.

- `app/pages/` — one folder per page, each has `.ts`, `.html`, and usually a `.service.ts`
- `app/security/` — `auth.service.ts`, `auth.guard.ts`, `auth.interceptor.ts` (adds JWT to all HTTP requests)
- `app/app.routes.ts` — all route definitions
- `app/app.config.ts` — `provideHttpClient` with `authInterceptor`

### Roles
- `ADMIN` — full user/professional management and audit log access
- `PROFESSIONAL` — manages own schedule (availability), prontuários, and documents
- `PATIENT` — views/confirms own appointments

### Appointment State Machine
`AGENDADA` → `CONFIRMADA_PROFISSIONAL` → `CONFIRMADA` → `CONCLUIDA` / `CANCELADA` / `FALTA`

The professional confirms first, then the patient must confirm. A `UNIQUE CONSTRAINT` on `(professional_id, date_time)` prevents race conditions at the DB level.

### Database Migrations
Flyway manages schema. Migration files are in `backend/src/main/resources/db/migration/` and run automatically on startup. Never modify existing migration files — always create a new `V{n}__description.sql`.

### Security Architecture
- **JWT**: Stateless; validated by `SecurityFilter` on every request
- **Passwords**: Argon2id via Spring Security's `Argon2PasswordEncoder`
- **Data at rest**: `EncryptionConverter` (AES-128 GCM) transparently encrypts sensitive JPA fields (prontuário notes, document data) via `@Convert`
- **Rate limiting**: Bucket4j in `RateLimitingFilter`
- **Audit logging**: `AuditLogService` records all sensitive data access
- **Email verification**: New accounts start unverified; 24h token sent via SMTP

### Environment Configuration
Copy `.env.dev` for local dev. Key variables:
- `SPRING_PROFILES_ACTIVE` — `dev` seeds fake data (DataInitializer), `prod` starts clean
- `ENCRYPTION_KEY` — exactly 16 characters for AES-128
- `JWT_SECRET` — 32+ character random string
- `INITIAL_ADMIN_EMAIL/PASSWORD` — credentials for first admin account (created by `AdminInitializer` if DB is empty)
- `ALLOWED_ORIGINS` — comma-separated CORS origins

## Coding Standards (from .cursorrules)

- **DTOs are Java Records** — never return `@Entity` from controllers
- **Constructor injection** — use Lombok `@RequiredArgsConstructor`; avoid `@Autowired` on fields
- **`var` for locals** — when the type is clear from context
- **Validation in DTOs** — Jakarta Bean Validation (`@NotNull`, `@Future`, etc.)
- **Global exception handling** — add new exception mappings to `GlobalExceptionHandler`, not in controllers
- **snake_case for DB** — all table/column names use snake_case

## Testing Notes

- Backend: JUnit integration tests in `backend/src/test/java/com/sistema/lucas/`
- Frontend unit tests: vitest (`*.spec.ts`)
- E2E tests: Cypress in `frontend/cypress/e2e/` — 13 suites covering the full user journey
- Cypress tests require containers to be running (`./deploy-dev.sh` first)
