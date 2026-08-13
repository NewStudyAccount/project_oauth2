# AGENTS.md

## Project Overview

OAuth2/OIDC SSO platform built with Spring Authorization Server. Four independent modules (no shared parent POM), each with its own `pom.xml` or `package.json`.

## Prerequisites

- **Java 21** (all Spring modules target `<java.version>21</java.version>`)
- **MySQL 8.0+** at `192.168.99.100:3306` (Docker default; change in `auth-server/src/main/resources/application.yml`)
- **Redis** at `192.168.99.100:6379` (database 12)
- **hosts entry**: `127.0.0.1 auth.local` — required for OAuth2 issuer URI validation

## Module Layout

| Module | Port | Role | Build |
|--------|------|------|-------|
| `auth-server/` | 9000 | Authorization Server (core) | `mvn spring-boot:run` |
| `app-vue/` | 5173 | Vue SPA (public client, PKCE) | `npm run dev` |
| `app-springboot/` | 8082 | Spring Boot app (confidential client) | `mvn spring-boot:run` |
| `resource-api/` | 8083 | Resource server (JWT validation) | `mvn spring-boot:run` |

**Start order**: `auth-server` first — all other modules depend on it for token issuance and JWKS.

## Build & Run Commands

```bash
# Auth Server (must be running before other modules)
cd auth-server && mvn spring-boot:run

# Vue frontend
cd app-vue && npm install && npm run dev

# Spring Boot client app
cd app-springboot && mvn spring-boot:run

# Resource API server
cd resource-api && mvn spring-boot:run
```

No test infrastructure exists yet (`src/test/` directories are empty or absent).

## Database

- Schema: `oauth2_center` (auto-created by `schema.sql`)
- Init scripts: `auth-server/src/main/resources/db/schema.sql` + `data.sql`
- Runs automatically on auth-server startup if MySQL is available
- Default admin: `admin` / `Admin@123` (BCrypt hashed in `data.sql`)

## Architecture Notes

### Package Structure (auth-server)
- `com.example.authserver.config/` — Spring Security + Authorization Server config
- `com.example.authserver.entity/` — MyBatis-Plus entities (Lombok `@Data`)
- `com.example.authserver.repository/` — MyBatis mappers (scanned via `@MapperScan`)
- `com.example.authserver.service/` — Business logic
- `com.example.authserver.controller/` — REST + page controllers

### Security Filter Chains
- `AuthorizationServerConfig` — `@Order(1)`: handles `/oauth2/*`, OIDC discovery, JWKS
- `SecurityConfig` — `@Order(2)`: form login, page auth, CSRF config
- CSRF disabled for `/api/**` and `/oauth2/**` paths

### JWT Keys
RSA keypair generated at startup in `AuthorizationServerConfig.keyPair()` — not persisted. Restarting auth-server invalidates all existing tokens.

### Vue App Proxy
`vite.config.ts` proxies `/userinfo` requests to `http://auth.local:9000`. The Vue app uses PKCE flow with `client_id: vue-app` (no client secret).

### MyBatis-Plus Config
- Underscore-to-camelCase mapping enabled
- Logical delete: `deleted` field (1=deleted, 0=active)
- ID strategy: auto-increment

## Conventions

- All Spring modules use Lombok (`@RequiredArgsConstructor`, `@Data`, etc.)
- Entity classes use MyBatis-Plus annotations, not JPA
- Thymeleaf templates in `src/main/resources/templates/` (auth-server, app-springboot)
- Vue app stores auth state in localStorage (tokens, user info)

## Key Files

- `auth-server/src/main/java/.../config/AuthorizationServerConfig.java` — OAuth2/OIDC core config
- `auth-server/src/main/java/.../config/SecurityConfig.java` — Security filter chains
- `auth-server/src/main/resources/db/schema.sql` — Full database schema
- `auth-server/src/main/resources/db/data.sql` — Initial clients + admin user
- `app-vue/src/utils/auth.js` — PKCE + token exchange logic
- `app-vue/vite.config.ts` — Dev server proxy config

## Gotchas

- The issuer URI `http://auth.local:9000` is hardcoded in multiple places (`application.yml`, `AuthorizationServerConfig.java`, Vue `auth.js`) — must match across all modules
- `springboot-app` client secret in `data.sql` is `Admin@123` (same as admin password, BCrypt hashed)
- No `mvnw`/`gradlew` wrappers — requires system Maven
- Redis database 12 is hardcoded in `application.yml`
