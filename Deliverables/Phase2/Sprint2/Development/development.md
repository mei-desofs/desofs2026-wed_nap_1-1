# Development - Phase 2, Sprint 2

This document records the **deltas** introduced in Sprint 2. The Sprint 1 baseline (technology stack, layered architecture, JWT validation, RBAC, DTOs, input validation, rate limiting, security headers, audit logging, error handling) remains in force and is documented in [Sprint 1 Development](../../Sprint1/Development/development.md).

## Table of Contents

1. [Sprint 2 Scope](#1-sprint-2-scope)
2. [UC5, UC6 and UC7 Controller and Service Refinements](#2-controller-and-service-refinements)
3. [UC8 - Manage Roles (User Administration & Token Invalidation)](#3-uc8---manage-roles-user-administration--token-invalidation)
4. [Containerisation & Production Image](#4-containerisation--production-image)
5. [Database Migrations](#5-database-migrations)

---

## 1. Sprint 2 Scope

| Area | Sprint 2 delta |
|---|---|
| Use Cases | UC5 (View Refund Requests), UC6 (Handle Refund Request), UC7 (Manage Movie Catalog) and UC8 (Manage Roles) |
| Authentication / Session | Server-side JWT denylist (`TokenInvalidationService` + `TokenFreshnessFilter`) and Auth0 session revocation on role change |
| Database | `V8__create_user_token_invalidations.sql`; schema-alignment migrations `V9__align_orders_schema.sql` and `V10__align_refund_requests_schema.sql` |
| Deployment | Hardened multi-stage `App/Dockerfile`, `App/docker-compose.yml` for local development, `docker-compose.prod.yml` for the production VM |

Stack, RBAC matrix and all transversal security controls are unchanged from Sprint 1.

---

## 2. Controller and Service Refinements

### 2.1 UC5 - View Refund Requests

**Location:** [`RefundController`](../../../../App/src/main/java/com/example/desofs/controllers/RefundController.java).

`GET /api/refunds` (list) and `GET /api/refunds/{id}` (detail) finalise UC5. Listing requires the `SUPPORT` role and is enforced via `RoleGuard` before any data access. Each successful list call is recorded by `AuditLogService` with operation `GET_REFUND_LIST`.

### 2.2 UC6 - Handle Refund Requests

**Location:** [`RefundController`](../../../../App/src/main/java/com/example/desofs/controllers/RefundController.java).

`PUT /{id}/approve` (refund) and `PUT /{id}/reject` (refund) finalise UC5. Approve or Reject a Refund Request requires the `SUPPORT` role and is enforced via `RoleGuard` before any data access. Each successful action (either approval or rejection) call is recorded by `AuditLogService` with operation `APPROVE_REFUND` or `REJECT_REFUND`, respectively.

### 2.3 UC7 - Manage Movie Catalog

**Location:** [`MovieController`](../../../../App/src/main/java/com/example/desofs/controllers/MovieController.java).

The catalog endpoints completed in Sprint 2 are:

| Endpoint | Role | Notes |
|---|---|---|
| `GET /api/movies` | `ADMIN` | Lists the catalog and emits `GET_MOVIE_LIST` audit entry |
| `GET /api/movies/{id}` | Authenticated | Returns 404 when the id does not exist |
| `POST /api/movies` | `ADMIN` | `@Valid MovieDTO`; `id` is forced to `null` to prevent client-supplied identifiers; emits `CREATE_MOVIE` |
| `PUT /api/movies/{id}` | `ADMIN` | `@Valid MovieDTO`; returns 404 when the movie is missing |

DTO validation (`@NotBlank`, `@Size`, `@DecimalMin`, `@Min`) and `GlobalExceptionHandler` continue to enforce input safety as documented in Sprint 1.

---

## 3. UC8 - Manage Roles (User Administration & Token Invalidation)

**Locations:** [`UserController`](../../../../App/src/main/java/com/example/desofs/controllers/UserController.java), [`UserService`](../../../../App/src/main/java/com/example/desofs/services/UserService.java), [`TokenFreshnessFilter`](../../../../App/src/main/java/com/example/desofs/security/TokenFreshnessFilter.java), `TokenInvalidationService`, `UserTokenInvalidation` (entity + repository), [`Auth0ManagementClient.invalidateSessions`](../../../../App/src/main/java/com/example/desofs/security/Auth0ManagementClient.java), `SecurityConfig` (filter wiring), and Flyway migration [`V8__create_user_token_invalidations.sql`](../../../../App/src/main/resources/db/migration/V8__create_user_token_invalidations.sql).

### 3.1 Endpoints

| Endpoint | Role | Notes |
|---|---|---|
| `GET /api/users` | `ADMIN` | Lists all users (read-through to Auth0 Management API) |
| `POST /api/users/{id}/roles` | `ADMIN` | Assigns the role provided in `RoleRequestDTO` to the target user; triggers token invalidation (Cap3.3) |
| `DELETE /api/users/{id}/roles` | `ADMIN` | Removes the role provided in `RoleRequestDTO`; triggers token invalidation (Cap3.3) |

`UserController` requires `ADMIN` via `RoleGuard` before any service call. The actor's identity is taken from the trusted JWT `sub` (`jwt.getSubject()`) and never from the request body. `UserService.guardSelfModification` rejects requests where the actor and target are the same user, preventing an admin from accidentally revoking their own session.

Audit entries (`AuditLogService`) are emitted for every successful role mutation as `ROLE_ASSIGNED:<ROLE>` / `ROLE_REMOVED:<ROLE>`.

### 3.2 Problem (token invalidation)

The API is a stateless JWT Resource Server with a 1 hour access-token lifetime (Sprint 1, Cap4.3). Without extra controls, a role change does not take effect until the next token issuance: an admin who removes the `ADMIN` role from a compromised account would still leave that account with up to one hour of admin access. ASVS V7.4.1 and V8.3.2 explicitly call this case out for self-contained tokens.

### 3.3 Design

On every administrative role change, `UserService` writes a per-user cut-off and asks Auth0 to drop the SSO session:

```java
private void invalidateUserSessions(String targetUserId, String reason) {
    tokenInvalidationService.invalidateTokensFor(targetUserId, reason);
    auth0.invalidateSessions(targetUserId);
}
```

The denylist is the **authoritative** control; the Auth0 call (`DELETE /api/v2/users/{id}/sessions`) is best-effort and only improves UX (silent-auth fails sooner). Any Auth0 failure is logged at WARN and swallowed so the DB transaction is not rolled back.

On every request, `TokenFreshnessFilter` (a `OncePerRequestFilter` placed after `BearerTokenAuthenticationFilter` and before `RateLimitFilter`) reads `jwt.sub` and `jwt.iat`, looks up the cut-off, and rejects with `401 invalid_token` (RFC 6750 `WWW-Authenticate: Bearer error="invalid_token"`) when `iat` is before the cut-off. Missing claims pass through (RBAC at the controller still applies).

### 3.4 Persistence

```sql
CREATE TABLE user_token_invalidations (
    auth0_user_id      VARCHAR(200) PRIMARY KEY,
    invalidated_after  TIMESTAMP(3) NOT NULL,
    reason             VARCHAR(100) NOT NULL,
    updated_at         TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
);
```

One row per user (cut-off, not a token list). `TIMESTAMP(3)` matches the millisecond precision of `Instant.now()` so a token issued in the same second as the revocation is correctly rejected.

### 3.5 Properties

- Role changes take effect on the next request.
- A refreshed token is only accepted if its new `iat` is after the cut-off.
- The denylist works even when Auth0 is unreachable.
- `userId` is taken from the trusted JWT `sub`; the cut-off is server-stamped (`Instant.now()`), so the caller cannot back-date it.
- The Auth0 Management API token must carry the `delete:sessions` scope; without it the session-revocation call is a no-op but the denylist still protects the API.

---

## 4. Containerisation & Production Image

**Locations:** [`App/Dockerfile`](../../../../App/Dockerfile), [`App/docker-compose.yml`](../../../../App/docker-compose.yml), [`docker-compose.prod.yml`](../../../../docker-compose.prod.yml).

`App/Dockerfile` is a two-stage build:

1. **Build stage** (`maven:3.9-eclipse-temurin-21`): resolves dependencies, packages the application JAR.
2. **Runtime stage** (`eclipse-temurin:21-jre`): copies the JAR into a slim JRE image, creates a dedicated system user `emovieshop`, drops to that user via `USER emovieshop`, and exposes only port 8080.

Local development runs through `App/docker-compose.yml`, which adds `security_opt: no-new-privileges:true`, `cap_drop: ALL`, and a `curl /actuator/health` healthcheck. Production deployment uses `docker-compose.prod.yml` consumed by the deploy job described in [Pipeline Automation Cap3](../PipelineAutomation/pipelineAutomation.md#3-deployment-pipeline).

Hardening summary (rationale and ASVS mapping in [Security Configuration & Installation](../SecurityConfigurationAndInstallation/securityConfigurationAndInstallation.md)):

- Non-root runtime user.
- JRE-only runtime image (build toolchain dropped at the runtime stage).
- No shell-level secrets in the image; database credentials are injected via SSH env at deploy time.
- Receipts directory mounted from the host (`/opt/emovieshop/receipts`) so application data survives container restarts.

---

## 5. Database Migrations

| Migration | Purpose |
|---|---|
| [`V8__create_user_token_invalidations.sql`](../../../../App/src/main/resources/db/migration/V8__create_user_token_invalidations.sql) | Denylist table backing UC8 token invalidation |
| [`V9__align_orders_schema.sql`](../../../../App/src/main/resources/db/migration/V9__align_orders_schema.sql) | Replaces legacy `orders.user_id` FK with `auth0_id`, adds `status`, `receipt_name`, `total_price` to match the JPA entity |
| [`V10__align_refund_requests_schema.sql`](../../../../App/src/main/resources/db/migration/V10__align_refund_requests_schema.sql) | Replaces legacy `refund_requests.user_id` FK with `auth0_id` |

V9 / V10 close a long-standing drift between the JPA entities (post-Auth0) and the schema created in V1, which had previously been masked by `hibernate.ddl-auto=create-drop`. With Flyway as the schema source of truth and `spring.jpa.hibernate.ddl-auto=validate` in production, these alignments are required for the application to start.
