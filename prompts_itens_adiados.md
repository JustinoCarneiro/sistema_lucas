# Prompts para Itens Adiados — Auditoria LGPD/Segurança

Use cada prompt abaixo em uma sessão futura para resolver o item correspondente.

---

## 1. AUD-01 — Rotação de Chave AES-128 para AES-256

```
Preciso rotacionar a chave de criptografia do sistema. Atualmente uso AES-128 com uma
chave fraca de 16 caracteres legível por humanos definida em ENCRYPTION_KEY no .env.

O que preciso:
1. Gerar uma nova chave AES-256 (32 bytes aleatórios codificados em Base64)
2. Criar um script Java/Spring Boot que:
   - Conecte no banco PostgreSQL de produção
   - Leia todos os campos cifrados (CPF, telefone, endereço, alergias, prontuários,
     documentos, reason, cancelReason, emergencyContactName, emergencyContactPhone)
   - Descriptografe cada campo com a chave ANTIGA
   - Re-cifre com a chave NOVA (AES-256-GCM)
   - Atualize o registro no banco
3. Atualizar o EncryptionConverter para aceitar chaves de 32 bytes (AES-256)
4. O script deve ser idempotente e ter um modo --dry-run para validação prévia

IMPORTANTE: faça backup antes de qualquer alteração. O arquivo de backup já existe
via ./backup.sh. O EncryptionConverter está em:
backend/src/main/java/com/sistema/lucas/config/jpa/EncryptionConverter.java
```

---

## 2. AUD-04 — Migração de Segredos para Docker Secrets

```
Preciso migrar os segredos do sistema de um arquivo .env em texto plano para
Docker Secrets. O sistema roda com Docker Compose (docker-compose.yml na raiz).

Segredos a migrar:
- DB_PASS (senha do PostgreSQL)
- JWT_SECRET (chave HMAC do JWT)
- ENCRYPTION_KEY (chave AES de cifragem)
- MAIL_PASSWORD (senha do Gmail SMTP)
- INITIAL_ADMIN_PASSWORD (senha do admin fundador)
- CPF_HASH_PEPPER (pepper do HMAC-SHA256 do CPF)

O que preciso:
1. Criar os Docker Secrets no docker-compose.yml
2. Atualizar o application.properties do Spring Boot para ler de /run/secrets/*
3. Atualizar o docker-compose.yml para montar os secrets nos containers
4. Criar um script de setup que gere os arquivos de secrets na primeira execução
5. Manter compatibilidade com o ambiente de desenvolvimento (.env.dev)

Arquivos relevantes:
- docker-compose.yml (raiz)
- backend/src/main/resources/application.properties
- .env (produção) e .env.dev (desenvolvimento)
```

---

## 3. SEC-01 — Migração de JWT localStorage para Cookie HttpOnly

```
Preciso migrar o armazenamento do token JWT de localStorage para cookie HttpOnly
para eliminar a vulnerabilidade a XSS. O sistema usa Angular 21 no frontend e
Spring Boot 3.4 no backend.

Estado atual:
- Login: frontend faz POST /auth/login, recebe { token: "..." } no JSON
- Armazenamento: localStorage.setItem('token', response.token)
- Interceptor: auth.interceptor.ts lê do localStorage e adiciona header Authorization
- Guard: auth.guard.ts verifica localStorage
- Logout: remove do localStorage

O que preciso:
1. Backend: alterar AuthController.login para retornar o token como cookie
   HttpOnly + Secure + SameSite=Strict (não no body JSON)
2. Backend: criar endpoint POST /auth/logout que limpa o cookie
3. Backend: alterar SecurityFilter para ler o token do cookie (além do header,
   para compatibilidade)
4. Frontend: remover todo uso de localStorage para token
5. Frontend: auth.interceptor.ts deve usar withCredentials: true
6. Frontend: auth.service.ts deve usar endpoint /auth/me para verificar sessão
7. Atualizar CORS para permitir credentials

Arquivos do frontend:
- src/app/security/auth.service.ts
- src/app/security/auth.guard.ts
- src/app/security/auth.interceptor.ts
- src/app/pages/login/login.ts
- src/app/pages/panel/panel.ts

Arquivos do backend:
- security/controller/AuthController.java
- security/config/SecurityFilter.java
- security/config/SecurityConfigurations.java
- security/service/TokenService.java
```

---

## 4. SEC-02 — Implementação Completa de MFA (TOTP) para Admin

```
A fundação do MFA já foi criada: as colunas mfa_enabled e totp_secret existem
na tabela users (migração V10) e os campos correspondentes estão na entidade
User.java.

Preciso agora implementar o fluxo completo:

1. Enrollment (ativação do MFA):
   - Endpoint POST /auth/mfa/setup (autenticado, role ADMIN)
   - Gerar TOTP secret, cifrar com EncryptionConverter, salvar no user
   - Retornar QR Code (otpauth:// URI) para o admin escanear no Google Authenticator
   - Endpoint POST /auth/mfa/verify-setup com o código TOTP para confirmar ativação
   - Setar mfaEnabled = true somente após verificação bem-sucedida

2. Verificação no Login:
   - Alterar AuthController.login: se user.isMfaEnabled(), NÃO retornar token ainda
   - Retornar { mfaRequired: true, mfaToken: "<token-temporário>" }
   - Novo endpoint POST /auth/mfa/verify que recebe mfaToken + código TOTP
   - Validar o TOTP contra o secret do user
   - Só então gerar e retornar o JWT definitivo

3. Frontend:
   - Tela de setup MFA no painel admin (com QR Code)
   - Tela intermediária no login para inserir código TOTP
   - auth.service.ts atualizado para o fluxo de dois passos

Dependência sugerida: com.warrenstrange:googleauth:1.5.0 (Maven)

Arquivos base:
- backend/src/main/java/com/sistema/lucas/model/User.java (já tem os campos)
- backend/src/main/java/com/sistema/lucas/security/controller/AuthController.java
- backend/src/main/java/com/sistema/lucas/security/config/SecurityFilter.java
- frontend/src/app/pages/login/login.ts
- frontend/src/app/security/auth.service.ts
```

---

## 5. SEC-03 — Refresh Token Rotativo com Denylist

```
Preciso implementar refresh token rotativo para permitir sessões seguras de
longa duração sem manter o access token com validade longa.

Arquitetura desejada:
1. Access token: 15 minutos (curto, stateless)
2. Refresh token: 7 dias (longo, rotativo, uso único)
3. Denylist: tabela no banco para tokens revogados (logout efetivo)

Backend:
- Criar entidade RefreshToken (id, token, userId, expiresAt, used, revokedAt)
- Criar RefreshTokenRepository
- Alterar AuthController.login para retornar { accessToken, refreshToken }
- Novo endpoint POST /auth/refresh que recebe refreshToken, valida,
  revoga o antigo e emite novo par (access + refresh)
- Novo endpoint POST /auth/logout que revoga o refresh token
- Criar TokenDenylistService com cache em memória + persistência
- Alterar SecurityFilter para verificar denylist antes de aceitar o access token
- Job agendado para limpar refresh tokens expirados (reuso do DataCleanupTask)

Frontend:
- auth.interceptor.ts: interceptar 401, tentar refresh automaticamente
- auth.service.ts: armazenar refresh token (se ainda em localStorage, ou cookie)
- Implementar retry queue para requests que falharam durante o refresh

Migração Flyway necessária para a tabela refresh_tokens.

Arquivos:
- security/service/TokenService.java
- security/config/SecurityFilter.java
- security/controller/AuthController.java
- frontend/src/app/security/auth.interceptor.ts
- frontend/src/app/security/auth.service.ts
```
