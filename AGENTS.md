# AGENTS.md — project_oauth2

## Project overview

OAuth2/OIDC SSO platform built on Spring Authorization Server. A Maven multi-module monorepo with 4 backend modules and 3 independent frontend apps.

## Architecture at a glance

| Module | Path | Port | Domain | Role |
|--------|------|------|--------|------|
| auth-server | auth-center/backend | 9000 | auth.local | Authorization Server (core) |
| admin-vue3 | auth-center/auth-server-frontend | 5174 | auth.local | Admin panel (Vue3+ElementPlus, Session auth) |
| app-springboot | client-app/backend | 8082 | client.a.local | Confidential OAuth2 Client + Resource Server |
| app-vue3-springboot | client-app/app-frontend | 5173 | client.a.local | Frontend for app-springboot (proxied) |
| app-vue | standalone-app/frontend | 5173 | client.b.local | Public Client (PKCE, self-manages tokens) |
| gateway | platform/gateway | 8080 | gateway.local | Spring Cloud Gateway + Token Relay |
| resource-api | platform/resource-api | 8083 | — | Pure Resource Server (JWT) |

## Required hosts entries

```
127.0.0.1  auth.local
127.0.0.1  client.a.local
127.0.0.1  client.b.local
127.0.0.1  gateway.local
```

## Infrastructure (docker-compose)

All infra lives in `infra/docker-compose.yml`. Default credentials: root/123456.

| Service | Port | Notes |
|---------|------|-------|
| MySQL 8.0 | 3306 | DB: oauth2_center |
| Redis 7 | 6379 | password: 123456, db 12 used by auth-server and gateway |
| Nacos 2.3.2 | 8848 | Service discovery for gateway + resource-api |

Start infra first: `docker compose -f infra/docker-compose.yml up -d`

## Build and run commands

### Backend (Maven, Java 21)

```bash
# From repo root — build all modules
mvn clean package -DskipTests

# Run individual modules
cd auth-center/backend && mvn spring-boot:run
cd client-app/backend && mvn spring-boot:run
cd platform/gateway && mvn spring-boot:run
cd platform/resource-api && mvn spring-boot:run
```

### Frontend (Node/npm)

```bash
# Each frontend is independent — always npm install first
cd auth-center/auth-server-frontend && npm install && npm run dev
cd client-app/app-frontend && npm install && npm run dev
cd standalone-app/frontend && npm install && npm run dev
```

## Critical implementation details

### OAuth2 clients are stored in the database, not config

Client configs live in `oauth2_registered_client` table, loaded by `JdbcRegisteredClientRepository`. The yml files contain zero client definitions — they were migrated to DB. To add a client, use the admin API (`POST /api/admin/clients`) or insert directly into the table.

### EnabledCheckingRegisteredClientRepository

`auth-center/backend/.../config/EnabledCheckingRegisteredClientRepository.java` wraps the standard repository. It checks `settings.client.enabled` in client settings — disabled clients return `null` (appear nonexistent to OAuth2 flows). This is NOT a Spring Authorization Server built-in; it's custom.

### Two security filter chains, ordered

1. `@Order(1)` — `authServerFilterChain`: OAuth2 endpoints (/oauth2/authorize, /token, /jwks, OIDC)
2. `@Order(2)` — `defaultFilterChain`: form login, API auth, everything else

CSRF is disabled for `/api/**` and `/oauth2/**`. API 401s return JSON; page 401s redirect to /login.

### Gateway is WebFlux — no spring-boot-starter-web

`platform/gateway` uses Spring Cloud Gateway (reactive). Never add `spring-boot-starter-web` to it — it will fail to start. The gateway uses Redis for distributed sessions and Nacos for service discovery routing (`lb://service-name`).

### Nacos is partially disabled

Auth-server's Nacos dependency is commented out in its pom.xml. Only gateway and resource-api register with Nacos. Auth-server is addressed directly by domain.

### JWK keys are generated at startup

RSA 2048-bit keypair is generated fresh each restart of auth-server. All previously issued JWTs become invalid on restart. This is fine for dev but would be a problem in production.

### MyBatis-Plus conventions

- Underscore-to-camelCase mapping enabled
- Logical delete: `deleted` field (1=deleted, 0=not)
- ID strategy: auto-increment
- SQL init is commented out by default in application.yml — run schema.sql manually

## Database schema

SQL files in `auth-center/backend/src/main/resources/db/`:
- `schema.sql` — full DDL (run first)
- `data.sql` — seed data (test accounts)
- Various `fix-password-*.sql` — password migration scripts

Key tables: `sys_user`, `oauth2_registered_client`, `oauth2_authorization`, `user_client_access`, `sys_audit_log`

## Frontend architecture notes

### admin-vue3 (auth-server-frontend)

- TypeScript (`tsconfig.json` present), Vue3 + Element Plus + Vue Router
- Vite proxies `/api`, `/login`, `/logout`, `/register`, `/consent`, `/send-code` to auth.local:9000
- Uses Session+Cookie auth (axios withCredentials), NOT OAuth2
- API layer in `src/api/` with separate files per domain (client.js, user.js, access.js, audit.js)

### app-vue3-springboot (client-app/app-frontend)

- Plain JS, Vue3, no TypeScript
- Proxies `/api`, `/oauth2`, `/login`, `/logout` to client.a.local:8082
- CSRF: reads XSRF-TOKEN cookie, sends as X-XSRF-TOKEN header

### app-vue (standalone-app/frontend)

- Vue3 + Pinia, no TypeScript
- PKCE flow: generates code_verifier/code_challenge, stores tokens in localStorage
- Only proxies `/userinfo` to auth.local:9000

## Port conflicts

Both `client-app/app-frontend` and `standalone-app/frontend` default to port 5173. They cannot run simultaneously without changing one vite config.

## Test account

| Username | Password | Role |
|----------|----------|------|
| admin | Admin@123 | ADMIN (full access to admin API) |

## Conventions that differ from defaults

- The admin API path is `/api/admin/**` (not `/admin/api/**`)
- Client IDs in the OAuth2 flow map to `oauth2_registered_client.id` (a UUID), NOT `client_id` (the string identifier). The `AdminController.findAllClientIds()` queries `id` column.
- `ClientConverter` in `auth-center/backend/.../dto/` handles all DTO↔entity mapping for RegisteredClient. It encodes client secrets via PasswordEncoder.
- Consent page is at `/consent` (Thymeleaf template), not a SPA route.

## Existing instruction files

- `architecture.md` — detailed architecture doc with all interaction flows
- `docs/` — additional analysis docs (auth-server-core-analysis, storage-extension, spring-cloud)
- `openspec/` — OpenSpec change management config
- `.agents/skills/` — OpenSpec skill definitions
