# Development - Phase 2, Sprint 1

## Table of Contents


1. [Technology Stack](#1-technology-stack)
2. [Architecture & Design](#2-architecture--design)
3. [Authentication & JWT Validation](#3-authentication--jwt-validation)
4. [Auth0 Configuration](#4-auth0-configuration)
5. [Role-Based Access Control (RBAC)](#5-role-based-access-control-rbac)
6. [Data Transfer Objects (DTOs)](#6-data-transfer-objects-dtos)
7. [Input Validation & Sanitization](#7-input-validation--sanitization)
8. [Rate Limiting & DoS Protection](#8-rate-limiting--dos-protection)
9. [Security HTTP Headers](#9-security-http-headers)
10. [Audit Logging](#10-audit-logging)
11. [Error Handling & Information Disclosure Prevention](#11-error-handling--information-disclosure-prevention)
12. [Token Invalidation & Session Revocation on Role Change](#12-token-invalidation--session-revocation-on-role-change)

---

## 1. Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.4.5 |
| Auth / Identity | Auth0 (external IdP) - JWT RS256 |
| Database | MySQL (production), H2 (tests) |
| ORM | Spring Data JPA / Hibernate |
| Rate Limiting | Bucket4j 8.10.1 (token-bucket algorithm) |
| Build | Maven |
| SAST | CodeQL (GitHub), SpotBugs + FindSecBugs, PMD |
| SCA | OWASP Dependency-Check 11.1.1 |
| DAST | OWASP ZAP (GitHub Actions) |
| Coverage | JaCoCo 0.8.12 |
| Testing | JUnit 5, Mockito, AssertJ, Spring Security Test |

## 2. Architecture & Design

This project follows a layered, testable design that embraces interface-driven development, clear separation of concerns, and pragmatic elements of Clean Architecture.

### 2.1 High-level architecture

- **Controllers (API adapters):** handle HTTP, authentication, validation and translate requests to application inputs. (See `App/src/main/java/com/example/desofs/controllers`)
- **Services (use-cases / application logic):** implemented behind interfaces (`IMovieService`, `IAuditLogService`, etc.). Services encapsulate business rules and orchestration. (See `App/src/main/java/com/example/desofs/services`)
- **Repositories / Gateways:** JPA repositories and Data Access Object layers isolate persistence concerns from business logic. (See `App/src/main/java/com/example/desofs/repositories`)
- **Domain:** entities and value objects representing core business concepts. (See `App/src/main/java/com/example/desofs/domain`)
- **DTOs & Mappers:** API surface uses DTOs; mappers convert between DTOs and domain entities. Mappers are kept explicit to avoid mass-assignment risks. (See `App/src/main/java/com/example/desofs/shared/dtos` and `App/src/main/java/com/example/desofs/shared/mappers`)

### 2.2 Interfaces and Dependency Inversion

- All service contracts are exposed as Java interfaces (e.g., `IMovieService`). Controllers depend on these interfaces, not concrete classes. This follows the **Dependency Inversion Principle** and makes units easy to mock in tests (`@MockitoBean IMovieService`).
- Example benefit: `MovieController` can be tested in isolation by mocking `IMovieService` and `IRoleGuard` without needing the full JPA stack.

### 2.3 Mappers

- Mappers live in `com.example.desofs.shared.mappers` and are intentionally explicit. They may be implemented manually or via a compile-time mapping tool (MapStruct) depending on complexity. The project currently uses straightforward mapping utilities to maintain clarity and avoid hidden mapping behaviour.
- Mappers protect the domain model by controlling which fields are mapped to/from external DTOs.

### 2.4 SOLID and Clean Architecture principles applied

- **Single Responsibility:** each controller, service and repository has a single focused responsibility (e.g., `ReceiptFileService` only handles receipt file creation and sanitization).
- **Open/Closed:** services expose behavior via interfaces so new behaviors can be added via new implementations without changing existing callers.
- **Liskov Substitution:** interfaces are designed so implementations are interchangeable (no surprising side-effects).
- **Interface Segregation:** small focused interfaces (service per aggregate) avoid forcing callers to implement unused methods.
- **Dependency Inversion:** high-level modules (controllers/use-cases) depend on abstractions (interfaces) rather than concrete persistence or framework specifics.

Clean Architecture mapping:

- **Entities (Domain):** business objects in `domain/`.
- **Use Cases (Application Services):** `services/` implement the application-specific business rules.
- **Interface Adapters:** `controllers/` and `shared/mappers` adapt input/output for use cases.
- **Frameworks & Drivers:** Spring Boot, JPA, Security live at the outermost layer and are injected via interfaces.

### 2.5 Examples & File locations

- Service interface: `App/src/main/java/com/example/desofs/services/IMovieService.java`
- Service impl: `App/src/main/java/com/example/desofs/services/MovieService.java`
- Guard interface: `App/src/main/java/com/example/desofs/security/IRoleGuard.java`
- DTOs: `App/src/main/java/com/example/desofs/shared/dtos/MovieDTO.java`
- Mappers: `App/src/main/java/com/example/desofs/shared/mappers/`

### 2.6 Rationale

This structure improves testability, supports safe swapping of implementations (for example, replacing JPA with a different persistence mechanism), reduces blast radius for changes, and makes security reviews and auditing easier because responsibilities are narrow and well-located.

---

## 3. Authentication & JWT Validation

**Location:** `App/src/main/java/com/example/desofs/config/SecurityConfig.java`

The application is a **stateless OAuth2 Resource Server**. It never handles passwords, authentication is fully delegated to **Auth0**.

### How it works

- Every request must carry a **Bearer JWT** in the `Authorization` header.
- Spring Security validates the token signature against Auth0's JWKS endpoint.
- `JwtValidators.createDefaultWithIssuer()` enforces **token expiration** (`exp`) and **not-before** (`nbf`) claims via the built-in `JwtTimestampValidator`, with a **60-second clock skew** tolerance. Expired tokens are rejected with `401 Unauthorized`.
- A custom `AudienceValidator` additionally verifies the JWT `aud` claim matches `emovieshop-api`, preventing token reuse from other Auth0 applications.
- Sessions are **stateless** (`SessionCreationPolicy.STATELESS`), no server-side session is ever created.
- CSRF protection is disabled (correct for a stateless JWT API, no cookies, no session).

```java
// Audience validation on top of issuer validation
OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(audience);
OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuerUri);
jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator));
```

### Public endpoints

| Endpoint | Reason |
|---|---|
| `GET /actuator/health` | Health checks from load balancers/orchestrators |

All other endpoints require a valid JWT.

---

## 4. Auth0 Configuration

Auth0 is the external Identity Provider (IdP) for eMovieShop. The following settings were configured in the Auth0 dashboard.

### 4.1 Roles

Three roles (`ADMIN`, `CUSTOMER`, `SUPPORT`) are defined in Auth0, each with a corresponding test user.

![Roles](../images/Roles.jpeg)

![Users](../images/Users.jpeg)

### 4.2 Post Login Action-Add Roles to Token

A **Post Login** custom action injects the user's Auth0 roles into the JWT under the namespace `https://emovieshop.com/roles`, and also attaches the user's email. The backend reads this claim in `RoleGuard` to enforce access control.

![Post Login flow](../images/Trigger.jpeg)

![Add roles to token action](../images/Action_add_roles_to_token.jpeg)

### 4.3 Token Lifetimes

Access token lifetime is set to **3600 s (1 hour)** for all flows.

![Access Token Expiration](../images/Access_Token_Expiration_Settings.jpeg)

Refresh tokens expire after **7 days of inactivity** (idle) with an absolute maximum of **~1 year**.

![Refresh Token Expiration](../images/Refresh_Token_Expiration_Settings.jpeg)

### 4.4 Refresh Token Rotation

Refresh tokens are **rotated on every use** with a 0 s overlap period-the old token is immediately invalidated, preventing replay attacks.

![Refresh Token Rotation](../images/Refresh_Token_Rotation_Settings.jpeg)

### 4.5 Attack Protection

Suspicious IP Throttling and Brute-force Protection are **enabled**. Bot Detection is disabled.

> **Note:** Bot detection (Auth0 Bot Detection feature) is **not activated** in this project. This feature requires a paid Auth0 plan and is therefore out of scope for this implementation. Controls related to automated attack mitigation that depend on bot detection remain unimplemented at the Auth0 level; brute-force protection (account lockout after 10 failed attempts) is still enforced via Auth0's free-tier Brute Force Protection.

![Attack Protection](../images/Attack_Protection_Settings.jpeg)

---

## 5. Role-Based Access Control (RBAC)

**Location:** `App/src/main/java/com/example/desofs/security/RoleGuard.java`

### Roles

| Role | Permissions |
|---|---|
| `CUSTOMER` | Create orders, request refunds |
| `SUPPORT` | View and process refund requests |
| `ADMIN` | Manage users, assign/remove roles, view audit logs |

### RoleGuard Pattern

Rather than using Spring's `@PreAuthorize`, controllers inject `RoleGuard` as a constructor dependency and call `requireRole(jwt, role)` explicitly at the start of each protected method. This makes the access control decision visible in the controller code and testable in isolation.

```java
@Component
public class RoleGuard {
    public void requireRole(Jwt jwt, Role requiredRole) {
        List<String> roles = jwt.getClaimAsStringList(rolesClaimNamespace);
        if (roles == null || !roles.contains(requiredRole.name())) {
            throw new AccessDeniedException("Access denied. Required role: " + requiredRole.name());
        }
    }
}
```

Roles are carried in a **custom Auth0 claim** namespaced as `https://emovieshop.com/roles` to avoid collision with standard JWT claims.

### Controller Access Matrix

| Controller | Endpoint | Required Role |
|---|---|---|
| `MovieController` | `GET /api/movies` | Any authenticated user |
| `MovieController` | `GET /api/movies/{id}` | Any authenticated user |
| `MovieController` | `POST /api/movies` | `ADMIN` |
| `OrderController` | `POST /api/orders` | `CUSTOMER` |
| `RefundController` | `POST /api/refunds` | `CUSTOMER` |
| `RefundController` | `GET /api/refunds` | Any authenticated user |
| `UserController` | `GET /api/users` | `ADMIN` |
| `UserController` | `POST /api/users/{id}/roles/assign` | `ADMIN` |
| `UserController` | `DELETE /api/users/{id}/roles/remove` | `ADMIN` |
| `AuditLogController` | `GET /api/audit-logs` | `ADMIN` |
| `AuditLogController` | `GET /api/audit-logs/{id}` | `ADMIN` |

---

## 6. Data Transfer Objects (DTOs)

**Location:** `App/src/main/java/com/example/desofs/shared/dtos/`

DTOs decouple the API surface from domain entities, preventing mass assignment vulnerabilities and controlling exactly what fields are exposed.

| DTO | Purpose |
|---|---|
| `PurchaseRequestDTO` | Incoming order creation payload |
| `PurchaseItemDTO` | Individual item in a purchase request |
| `OrderResponseDTO` | Order confirmation returned to client |
| `OrderItemResponseDTO` | Item detail in order response |
| `RefundRequestDTO` | Refund request representation |
| `CreateRefundRequest` | Incoming refund creation payload |
| `RejectRefundRequest` | Payload for refund rejection with reason |
| `UserDTO` | Safe user representation for admin endpoints |
| `RoleRequestDTO` | Role assignment/removal payload |
| `MovieDTO` | Movie representation |

---

## 7. Input Validation & Sanitization

Input security uses two complementary layers:

### 7.1 Input Validation (Bean Validation / Jakarta)

**Location:** controller methods, DTOs in `App/src/main/java/com/example/desofs/shared/dtos/`, and domain entities in `App/src/main/java/com/example/desofs/domain/`

All user-facing input is validated **declaratively** using Jakarta Bean Validation annotations (`jakarta.validation.constraints.*`). This approach ensures that invalid data is rejected at the API boundary, before it reaches services or the database - producing a consistent `400 Bad Request` response with field-level error details.

#### Why Jakarta Bean Validation

- **Fail-fast at the boundary** - constraints are evaluated by the framework as soon as the request body is deserialized, so no business logic or database call is executed with invalid data.
- **Declarative and co-located** - validation rules live next to the fields they protect, making them easy to read and audit.
- **Consistent error format** - all constraint violations are caught by the `GlobalExceptionHandler` (`MethodArgumentNotValidException`) and returned in the standard JSON error structure with field-level messages.

#### How it is applied

Controllers annotate `@RequestBody` parameters with `@Valid`. This triggers the validation cascade on the incoming payload:

```java
// MovieController
public ResponseEntity<Movie> create(@AuthenticationPrincipal Jwt jwt,
                                     @Valid @RequestBody Movie m) { ... }

// OrderController
public ResponseEntity<OrderResponseDTO> createOrder(@AuthenticationPrincipal Jwt jwt,
                                                     @Valid @RequestBody PurchaseRequestDTO request) { ... }

// RefundController
public ResponseEntity<RefundRequestDTO> createRefundRequest(@AuthenticationPrincipal Jwt jwt,
                                                             @Valid @RequestBody CreateRefundRequest request) { ... }
```

#### Constraints used across the codebase

The following annotations are applied on DTOs and domain entities:

| Annotation | Purpose | Example |
|---|---|---|
| `@NotNull` | Field must be present | `price` (Movie), `movieId` (PurchaseItemDTO), `orderId` (CreateRefundRequest) |
| `@NotBlank` | String must be non-null and non-empty | `title` (Movie), `receiptName` (PurchaseRequestDTO), `reason` (CreateRefundRequest) |
| `@Size(max=N)` | Bounds string/collection length | `description` max 5000, `genre` max 100, `items` max 10 (PurchaseRequestDTO) |
| `@Min(N)` | Numeric minimum | `stockQuantity >= 0` (Movie), `quantity >= 1` (PurchaseItemDTO) |
| `@DecimalMin` | Decimal minimum (exclusive) | `price > 0.0` (Movie) |
| `@Valid` (nested) | Cascades validation into child objects | `items` list in PurchaseRequestDTO validates each PurchaseItemDTO |

### 7.2 Input Sanitization (Receipt File Service & Path Traversal Protection)

**Location:** `App/src/main/java/com/example/desofs/services/ReceiptFileService.java`

When an order is created, a receipt file is written to a sandboxed directory. The service protects against file system attacks through:

1. **Allow-list sanitization** - strips all characters outside `[a-zA-Z0-9 _-]`
2. **Maximum name length** - enforced before file creation (default 100 chars)
3. **Path canonicalization** - the resolved output path is verified to remain inside the configured receipts directory, blocking `../` traversal even after URL-decoding
4. **Dedicated sandbox directory** - receipts are never written outside the configured base path

The `ReceiptFileService.sanitizeReceiptName()` applies a **multi-step allow-list pipeline** to every receipt name before it is used as a filesystem filename. The steps run in order:

#### Step 1 - Null byte stripping

```java
sanitized = rawName.replace("\0", "");
```

Null bytes (`\0`) can trick OS-level filename parsing. For example, `receipt.txt\0.jpg` may be interpreted as `receipt.txt` by the JVM but as `receipt.txt\0.jpg` by the underlying C library. Stripping them first prevents this class of injection entirely.

#### Step 2 - Allow-list character filter

```java
private static final String ALLOWED_CHARS_PATTERN = "[^a-zA-Z0-9 _-]";
sanitized = sanitized.replaceAll(ALLOWED_CHARS_PATTERN, "");
```

Only characters in `[a-zA-Z0-9 _-]` survive. Every character outside this set is **stripped**, not escaped. This is a deny-by-default approach: anything not explicitly allowed is removed. The table below shows the threat categories blocked by this single rule:

| Blocked character class | Examples | Threat mitigated |
|---|---|---|
| Path separators | `/`, `\`, `:` | Path traversal (`../receipts/../etc/passwd`) |
| URL-encoded traversal | `%2F`, `%2E%2E` | Double-encoded path traversal |
| Shell metacharacters | `;`, `|`, `&`, `` ` ``, `$`, `(`, `)` | OS command injection via filename |
| Glob / wildcard | `*`, `?`, `[`, `]` | Filesystem glob expansion |
| Null-byte remnants | `%00` literals | Secondary null-byte injection |
| XML / HTML special chars | `<`, `>`, `"`, `'` | Reflected XSS in receipt listings / log injection |
| Dot sequences | `..` (both dots stripped individually) | Residual traversal after decoding |
| Unicode homoglyphs & control chars | U+202E (RTL override), U+0000–U+001F | Filename spoofing, log poisoning |

#### Step 3 - Whitespace trim

```java
sanitized = sanitized.trim();
```

Prevents names composed entirely of spaces passing through to the filesystem.

#### Step 4 - Length truncation

```java
if (sanitized.length() > maxNameLength) {   // default: 100
    sanitized = sanitized.substring(0, maxNameLength);
}
```

Caps the filename at 100 characters (configurable via `emovieshop.receipts.max-name-length`). This guards against:
- **Filesystem limits** - most filesystems cap filenames at 255 bytes; staying well below avoids edge cases with multi-byte characters.
- **Log injection via long strings** - overly long filenames can corrupt structured log parsers.

#### Step 5 - Post-sanitization blank guard

```java
if (sanitized.isBlank()) {
    throw new IllegalArgumentException("Receipt name contains no valid characters after sanitization");
}
```

If all characters were stripped (e.g., input was `"../../"` or `";;;"`), the request is rejected with a `400 Bad Request` rather than creating a file with an empty or auto-generated name silently.

---

## 8. Rate Limiting & DoS Protection

**Location:** `App/src/main/java/com/example/desofs/security/RateLimitFilter.java`

Rate limiting is implemented as a **servlet filter** using the **token-bucket algorithm** (Bucket4j 8.10.1). It is registered in the Spring Security filter chain immediately after JWT authentication, so the authenticated user identity is available for per-user limits.

### Two independent limits applied per request

| Layer | Key | Default limit |
|---|---|---|
| Per-IP | `X-Forwarded-For` / remote address | 300 req/min |
| Per-user | JWT `sub` claim | 120 req/min |

When either limit is exceeded, the filter returns **HTTP 429 Too Many Requests** and logs a warning with the IP or user ID, without exposing internal details to the client.

Both limits are configurable via `application.properties`:

```properties
emovieshop.rate-limit.ip.requests-per-minute=300
emovieshop.rate-limit.user.requests-per-minute=120
```

---

## 9. Security HTTP Headers

**Location:** `App/src/main/java/com/example/desofs/security/SecurityHeadersFilter.java`

Every response includes the following security headers:

| Header | Value | Protection |
|---|---|---|
| `X-Content-Type-Options` | `nosniff` | MIME-type sniffing |
| `X-Frame-Options` | `DENY` | Clickjacking |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` | SSL stripping |
| `Content-Security-Policy` | `default-src 'self'; frame-ancestors 'none'` | XSS / framing |
| `X-XSS-Protection` | `1; mode=block` | Reflected XSS (legacy browsers) |
| `Referrer-Policy` | `no-referrer` | Referrer leakage |
| `Cross-Origin-Resource-Policy` | `same-origin` | Spectre side-channel attacks |

These are also configured at the Spring Security `HttpSecurity` level (HSTS, frame options, content-type options), providing defence-in-depth.

### CORS (Cross-Origin Resource Sharing)

CORS is handled with **origin validation** rather than a permissive wildcard (`*`). The filter reads the `Origin` header from each request and only adds CORS response headers if the origin is present in a configurable allowlist.

```java
private void addCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
    String origin = request.getHeader("Origin");
    if (origin != null && allowedOrigins.contains(origin)) {
        response.setHeader("Access-Control-Allow-Origin", origin);
        response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Authorization,Content-Type,X-Requested-With");
        response.setHeader("Access-Control-Max-Age", "3600");
    }
}
```

Allowed origins are configured via `application.properties`:

```properties
emovieshop.cors.allowed-origins=${CORS_ALLOWED_ORIGINS:http://localhost:3000}
```

Multiple origins can be specified as a comma-separated list. Requests from unlisted origins receive no CORS headers, causing the browser to block the cross-origin request.

---

## 10. Audit Logging

**Location:** `App/src/main/java/com/example/desofs/domain/AuditLog.java`, `App/src/main/java/com/example/desofs/services/AuditLogService.java`

Security-sensitive operations are persisted to a dedicated `audit_logs` database table. This supports ASVS V16 (Security Logging) requirements.

### What is logged

| Event | Logged fields |
|---|---|
| Role assigned to user | `actorId`, `targetUserId`, `role`, `operation=ASSIGN`, `timestamp` |
| Role removed from user | `actorId`, `targetUserId`, `role`, `operation=REMOVE`, `timestamp` |
| Movie created | `actorId`, `targetUserId`, `role`, `operation=CREATE_MOVIE`, `timestamp` |
| Order created | `actorId`, `targetUserId`, `role`, `operation=CREATE_ORDER`, `timestamp` |
| Refund requested | `actorId`, `targetUserId`, `role`, `operation=CREATE_REFUND_REQUEST`, `timestamp` |
| Refund approved | `actorId`, `targetUserId`, `role`, `operation=APPROVE_REFUND`, `timestamp` |
| Refund rejected | `actorId`, `targetUserId`, `role`, `operation=REJECT_REFUND`, `timestamp` |

The `id` field has no public setter, it is assigned only by JPA after persistence, preventing audit record tampering.

For resource-creation events where there is no separate target user, the current implementation stores the authenticated subject in both `actorId` and `targetUserId` so the event remains traceable.

### Querying audit logs

`GET /api/audit-logs`, restricted to `ADMIN` role, returns the full audit trail.

> **Current scope:** logging is implemented for role changes, movie creation, order creation, and refund lifecycle events. Additional event types can be added later if the audit policy expands.

---

## 11. Error Handling & Information Disclosure Prevention

**Location:** `App/src/main/java/com/example/desofs/exceptions/GlobalExceptionHandler.java`

A `@RestControllerAdvice` centralizes all error responses into a consistent JSON format, preventing stack traces or internal details from leaking to clients.

### Response format

Every error response follows the same structure:

```json
{
  "correlationId": "uuid",
  "status": 400,
  "message": "Human-readable message",
  "timestamp": "2026-05-15T12:00:00"
}
```

The `correlationId` allows operators to trace an error in server logs without exposing internals to the client.

### Exception mapping

| Exception | HTTP Status | Message exposed |
|---|---|---|
| `MethodArgumentNotValidException` | 400 | Field-level validation errors |
| `IllegalArgumentException` | 400 | Business input error |
| `HttpMessageNotReadableException` | 400 | "Malformed request body" |
| `DataIntegrityViolationException` | 400 | "Invalid data: a required field is missing or violates constraints" |
| `SecurityException` | 400 | "Invalid request" (generic) |
| `AccessDeniedException` | 403 | "Access denied" |
| `NoResourceFoundException` | 404 | "Resource not found" |
| `HttpRequestMethodNotSupportedException` | 405 | "Method not allowed" |
| `IllegalStateException` | 409 | Conflict message |
| `HttpMediaTypeNotSupportedException` | 415 | "Unsupported media type" |
| `Exception` (catch-all) | 500 | "An unexpected error occurred" |

### Custom Error Controller

**Location:** `App/src/main/java/com/example/desofs/controllers/CustomErrorController.java`

Spring Boot's default `BasicErrorController` returns `text/html` error pages for requests that fall outside the DispatcherServlet scope (e.g., requests to the root path or unknown paths without a matching controller). Since eMovieShop is a pure REST API, a custom `ErrorController` implementation replaces this default behaviour, ensuring **all error responses are returned as `application/json`**, regardless of the request path or the client's `Accept` header.

The controller maps `GET` and `HEAD` on `/error` (Spring internally forwards to this path) and returns the same JSON structure used by the `GlobalExceptionHandler`.

### Security properties

- **No stack traces** - `server.error.include-stacktrace=never` in `application.properties`
- **No internal messages** - `server.error.include-message=never`
- **Generic catch-all** - unexpected exceptions always return a safe generic message; the real error is logged server-side with the correlation ID
- **Consistent JSON Content-Type** - all error responses from both `GlobalExceptionHandler` and `CustomErrorController` explicitly set `Content-Type: application/json`, preventing content-type mismatch issues
- **404 for unknown paths** - requests to undefined endpoints return 404 (not 500), preventing path enumeration from triggering noisy error responses
- **Database constraint errors** - `DataIntegrityViolationException` is caught and returns 400 with a safe message, preventing SQL/schema details from leaking

---

## 12. Token Invalidation & Session Revocation on Role Change

**Locations:**
- `App/src/main/java/com/example/desofs/security/TokenFreshnessFilter.java`
- `App/src/main/java/com/example/desofs/services/TokenInvalidationService.java`
- `App/src/main/java/com/example/desofs/services/ITokenInvalidationService.java`
- `App/src/main/java/com/example/desofs/domain/UserTokenInvalidation.java`
- `App/src/main/java/com/example/desofs/repositories/UserTokenInvalidationRepository.java`
- `App/src/main/java/com/example/desofs/security/Auth0ManagementClient.java` (method `invalidateSessions`)
- `App/src/main/java/com/example/desofs/services/UserService.java` (hook on `assignRole` / `removeRole`)
- `App/src/main/java/com/example/desofs/config/SecurityConfig.java` (filter wiring)
- `App/src/main/resources/db/migration/V8__create_user_token_invalidations.sql`

### 12.1 Problem statement

The API is a **stateless OAuth2 Resource Server** with access tokens issued by Auth0 and a lifetime of 3600 s (see §4.3). With pure JWT validation, **any token issued before a privilege change remains valid until it expires**. The most damaging case is:

1. An administrator removes the `ADMIN` role from a compromised account.
2. The attacker still holds an unexpired access token that contains `roles: ["ADMIN"]`.
3. For up to one hour the attacker continues to invoke privileged endpoints (`POST /api/users/{id}/roles/assign`, `POST /api/movies`, etc.) because the JWT signature is still valid and the `roles` claim is read from the token itself.

The same window applies when a role is **assigned**: a freshly granted role does not appear in any token the user already holds, so the user must wait for a new token or log out and back in.

This is exactly the case ASVS V7.4.1 calls out for self-contained tokens: the application must implement a per-user denylist, a per-user cut-off date, or a per-user signing key rotation. We adopted the **per-user cut-off date** approach because it is the cheapest at request time (one indexed primary-key lookup) and requires no changes to the IdP.

### 12.2 Design

Two complementary controls run on every administrative role change:

1. **Server-side denylist (authoritative).** A row is upserted in the `user_token_invalidations` table with the `auth0_user_id` and a UTC timestamp `invalidated_after`. A request filter then rejects any JWT whose `iat` (issued-at) claim is **before** that cut-off, regardless of whether the JWT signature is still valid.
2. **Auth0 session revocation (best-effort).** A `DELETE /api/v2/users/{id}/sessions` call is issued to the Auth0 Management API. This drops the IdP-side SSO session so the SPA's silent-auth refresh will fail and the user will be redirected to re-login. The Auth0 call is fire-and-forget: failures are logged but never propagate, because the denylist is the actual security control.

The two controls together give the property that **role changes take effect on the next request**, not on the next token refresh.

### 12.3 Component overview

```
            ┌────────────────────────┐
   admin -> │ UserController         │
            │  POST .../roles/assign │
            └──────────┬─────────────┘
                       │
                       v
            ┌────────────────────────────────────────────────┐
            │ UserService.assignRole(target, role)            │
            │  1. auth0.assignRole(...)            (IdP)      │
            │  2. auditLog.log(...)                (DB)       │
            │  3. invalidateUserSessions(target, reason)      │
            │       a. tokenInvalidation.invalidateTokensFor  │
            │           -> upsert user_token_invalidations    │
            │       b. auth0.invalidateSessions   (best effort│
            │           -> DELETE /users/{id}/sessions  )     │
            └────────────────────────────────────────────────┘

  Every subsequent request from the affected user:

    Bearer JWT ── BearerTokenAuthenticationFilter ──> SecurityContext
                                                            │
                                                            v
                                  TokenFreshnessFilter (new in this sprint)
                                  if jwt.iat < cutoff -> 401 invalid_token
                                                            │
                                                            v
                                                    RateLimitFilter
                                                            │
                                                            v
                                                    Controller
```

### 12.4 Persistence model

Migration `V8__create_user_token_invalidations.sql` (Flyway):

```sql
CREATE TABLE IF NOT EXISTS user_token_invalidations (
    auth0_user_id      VARCHAR(200) NOT NULL,
    invalidated_after  TIMESTAMP(3) NOT NULL,
    reason             VARCHAR(100) NOT NULL,
    updated_at         TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (auth0_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

Design notes:
- **Primary key on `auth0_user_id`** keeps the table size bounded by the user count (not by request volume) and gives O(1) lookup per request.
- `TIMESTAMP(3)` (millisecond precision) is required because JWT `iat` is in seconds but `invalidated_after` is stamped with `Instant.now()`; using whole-second precision would allow a JWT issued in the same second to be incorrectly treated as fresh.
- `reason` is a short string (e.g. `ROLE_ASSIGNED:ADMIN`, `ROLE_REMOVED:SUPPORT`) used for audit/forensics, not for access decisions.
- A single row per user is sufficient: the denylist is a **per-user cut-off**, not a list of revoked tokens. Subsequent invalidations overwrite the timestamp.

### 12.5 Token freshness filter

`TokenFreshnessFilter` is a `OncePerRequestFilter` registered **after** Spring Security's `BearerTokenAuthenticationFilter` (so the JWT is already parsed and validated) and **before** `RateLimitFilter` (so revoked tokens do not consume the user's rate quota).

```java
@Override
protected void doFilterInternal(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain chain) throws ServletException, IOException {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
        chain.doFilter(request, response);
        return;
    }
    Jwt jwt = jwtAuth.getToken();
    String userId = jwt.getSubject();
    Instant issuedAt = jwt.getIssuedAt();
    if (userId == null || issuedAt == null) {
        chain.doFilter(request, response);
        return;
    }
    if (invalidationService.isTokenInvalidated(userId, issuedAt)) {
        SecurityContextHolder.clearContext();
        writeUnauthorized(response);
        return;
    }
    chain.doFilter(request, response);
}
```

Reject response (matches RFC 6750):

```
HTTP/1.1 401 Unauthorized
WWW-Authenticate: Bearer error="invalid_token", error_description="Token has been invalidated; please reauthenticate"
Content-Type: application/json

{"error":"invalid_token","message":"Token has been invalidated; please reauthenticate."}
```

### 12.6 Service contract

`ITokenInvalidationService` exposes a minimal three-method API so the filter and `UserService` depend only on what they need:

| Method | Purpose |
|---|---|
| `invalidateTokensFor(String userId, String reason)` | Upsert the cutoff row; idempotent. Used by `UserService` on role change. `@Transactional`. |
| `getInvalidatedAfter(String userId)` | Read-only lookup of the cutoff timestamp (used for diagnostics/testing). |
| `isTokenInvalidated(String userId, Instant tokenIssuedAt)` | Returns `true` iff a cutoff exists and `tokenIssuedAt.isBefore(cutoff)`. Tolerates `null` inputs (returns `false`) so the filter never throws. |

The implementation (`TokenInvalidationService`) keeps the write path simple:
- Validates `userId` is non-blank (defensive: prevents poisoning the table with an empty key).
- Normalises a blank `reason` to `"UNSPECIFIED"` so the column constraint cannot be violated by an upstream caller.
- Stamps `invalidatedAfter = Instant.now()` server-side; the caller cannot back-date the cutoff.
- Logs an INFO event without exposing the user identifier (the audit trail already carries it).

### 12.7 Hook in `UserService`

Role assignment and removal share a single private helper so the two paths cannot drift:

```java
private void invalidateUserSessions(String targetUserId, String reason) {
    tokenInvalidationService.invalidateTokensFor(targetUserId, reason);
    auth0.invalidateSessions(targetUserId);
}
```

The order is intentional: the denylist write happens **first** so that even if the Auth0 call later fails, the authoritative control is already in place. `assignRole` calls it with `"ROLE_ASSIGNED:" + role.name()` and `removeRole` with `"ROLE_REMOVED:" + role.name()`. The call sits **after** the audit-log entry so the audit record is persisted before any side-effect on the user's session.

### 12.8 Auth0 session revocation

`Auth0ManagementClient.invalidateSessions(String userId)` issues:

```
DELETE https://{tenant}/api/v2/users/{auth0UserId}/sessions
Authorization: Bearer {management-api-token}
```

This is best-effort by design:
- Any `Auth0ManagementException` (HTTP error, network failure, missing scope) is caught and logged at WARN level.
- The method always returns normally so the database transaction in `UserService` is not rolled back by a transient IdP error.
- The defensive guarantee is provided by the denylist; the Auth0 call only improves UX (silent-auth fails sooner, the SPA redirects to login instead of holding a dead session).

### 12.9 Filter chain wiring

`SecurityConfig` adds the new filter immediately after the OAuth2 resource-server's bearer-token filter:

```java
http
    .oauth2ResourceServer(o -> o.jwt(j -> j.decoder(jwtDecoder())))
    .addFilterAfter(tokenFreshnessFilter, BearerTokenAuthenticationFilter.class)
    .addFilterAfter(rateLimitFilter, TokenFreshnessFilter.class);
```

This places the freshness check **after** JWT signature/expiry validation (so we do not waste a DB call on tokens that are already invalid for other reasons) and **before** rate limiting (so invalidated tokens do not consume the rate-limit budget).

### 12.10 Security properties and trade-offs

| Property | How it is achieved |
|---|---|
| Role changes take effect on the next request | Filter compares `iat` against the per-user cutoff on every request. |
| Cannot be bypassed by a stolen refresh token | A token refresh produces a new JWT with a *new* `iat` only if `iat > cutoff` at issuance time; otherwise it is rejected the same way. The Auth0 session revocation additionally prevents silent-auth refreshes. |
| Cannot be bypassed by client-side caching | The check happens server-side on every authenticated request. |
| Does not leak PII | Logs use the operation reason (e.g. `ROLE_ASSIGNED:ADMIN`); the user id is not written to application logs. |
| Cannot poison the table | `userId` is validated non-blank and pulled from the trusted JWT `sub` in the call chain; the column is bounded at 200 chars. |
| Defence in depth | The denylist is authoritative even when the Auth0 Management API is unavailable. |
| Fail-open vs fail-closed | The filter is **fail-closed for revoked tokens** (rejects) and **fail-open for missing claims** (passes through - the request would still be rejected later if it lacks the role required by the controller). |
| Clock-skew safety | Both `iat` and `invalidatedAfter` are compared as UTC `Instant`s with millisecond precision; no per-server clock arithmetic is performed. |

### 12.11 Operational notes

- **Schema rollout.** Flyway applies `V8__create_user_token_invalidations.sql` on startup. No manual step is required.
- **Required Auth0 scope.** The Management API token used by `Auth0ManagementClient` must include `delete:sessions` for the session-revocation call. If the scope is missing, the call logs a WARN but the denylist still protects the API.
- **Re-login UX.** After a role change the affected user receives `401 invalid_token` on their next request; the SPA's standard 401 handler triggers a silent-auth attempt, which fails (because the Auth0 session is gone) and falls back to interactive login. The new token will carry the updated roles.
- **Recovery.** Removing a row from `user_token_invalidations` re-enables all currently valid tokens for that user; this should never be required in normal operation and is intentionally not exposed as an API.
