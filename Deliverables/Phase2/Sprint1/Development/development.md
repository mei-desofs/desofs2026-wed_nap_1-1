# Development - Phase 2, Sprint 1

## Table of Contents

1. [Technology Stack](#1-technology-stack)
2. [Authentication & JWT Validation](#2-authentication--jwt-validation)
3. [Auth0 Configuration](#3-auth0-configuration)
4. [Role-Based Access Control (RBAC)](#4-role-based-access-control-rbac)
5. [Data Transfer Objects (DTOs)](#5-data-transfer-objects-dtos)
6. [Input Validation & Sanitization](#6-input-validation--sanitization)
7. [Rate Limiting & DoS Protection](#7-rate-limiting--dos-protection)
8. [Security HTTP Headers](#8-security-http-headers)
9. [Audit Logging](#9-audit-logging)
10. [Error Handling & Information Disclosure Prevention](#10-error-handling--information-disclosure-prevention)

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
| IAST | Not implemented (planned) |
| Coverage | JaCoCo 0.8.12 |
| Testing | JUnit 5, Mockito, AssertJ, Spring Security Test |

---

## 2. Authentication & JWT Validation

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

## 3. Auth0 Configuration

Auth0 is the external Identity Provider (IdP) for eMovieShop. The following settings were configured in the Auth0 dashboard.

### 3.1 Roles

Three roles (`ADMIN`, `CUSTOMER`, `SUPPORT`) are defined in Auth0, each with a corresponding test user.

![Roles](../images/Roles.jpeg)

![Users](../images/Users.jpeg)

### 3.2 Post Login Action-Add Roles to Token

A **Post Login** custom action injects the user's Auth0 roles into the JWT under the namespace `https://emovieshop.com/roles`, and also attaches the user's email. The backend reads this claim in `RoleGuard` to enforce access control.

![Post Login flow](../images/Trigger.jpeg)

![Add roles to token action](../images/Action_add_roles_to_token.jpeg)

### 3.3 Token Lifetimes

Access token lifetime is set to **3600 s (1 hour)** for all flows.

![Access Token Expiration](../images/Access_Token_Expiration_Settings.jpeg)

Refresh tokens expire after **7 days of inactivity** (idle) with an absolute maximum of **~1 year**.

![Refresh Token Expiration](../images/Refresh_Token_Expiration_Settings.jpeg)

### 3.4 Refresh Token Rotation

Refresh tokens are **rotated on every use** with a 0 s overlap period-the old token is immediately invalidated, preventing replay attacks.

![Refresh Token Rotation](../images/Refresh_Token_Rotation_Settings.jpeg)

### 3.5 Attack Protection

Suspicious IP Throttling and Brute-force Protection are **enabled**. Bot Detection is disabled.

> **Note:** Bot detection (Auth0 Bot Detection feature) is **not activated** in this project. This feature requires a paid Auth0 plan and is therefore out of scope for this implementation. Controls related to automated attack mitigation that depend on bot detection remain unimplemented at the Auth0 level; brute-force protection (account lockout after 10 failed attempts) is still enforced via Auth0's free-tier Brute Force Protection.

![Attack Protection](../images/Attack_Protection_Settings.jpeg)

---

## 4. Role-Based Access Control (RBAC)

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

## 5. Data Transfer Objects (DTOs)

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

## 6. Input Validation & Sanitization

Input security uses two complementary layers:

### 6.1 Input Validation (Bean Validation / Jakarta)

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

### 6.2 Input Sanitization (Receipt File Service & Path Traversal Protection)

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

## 7. Rate Limiting & DoS Protection

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

## 8. Security HTTP Headers

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

## 9. Audit Logging

**Location:** `App/src/main/java/com/example/desofs/domain/AuditLog.java`, `App/src/main/java/com/example/desofs/services/AuditLogService.java`

Security-sensitive administrative operations are persisted to a dedicated `audit_logs` database table. This supports ASVS V16 (Security Logging) requirements.

### What is logged

| Event | Logged fields |
|---|---|
| Role assigned to user | `actorId`, `targetUserId`, `role`, `operation=ASSIGN`, `timestamp` |
| Role removed from user | `actorId`, `targetUserId`, `role`, `operation=REMOVE`, `timestamp` |

The `id` field has no public setter, it is assigned only by JPA after persistence, preventing audit record tampering.

### Querying audit logs

`GET /api/audit-logs`, restricted to `ADMIN` role, returns the full audit trail.

> **Known gap (TODO):** Phase 1 documentation also mandates logging for refund decisions and catalog edits. This is not yet implemented.

---

## 10. Error Handling & Information Disclosure Prevention

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
