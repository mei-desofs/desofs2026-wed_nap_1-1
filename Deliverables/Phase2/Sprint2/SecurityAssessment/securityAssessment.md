# Security Assessment - Phase 2, Sprint 2

This document records the security assessment activities performed at the end of Phase 2. Per the rubric (Sprint 2, *Operate* criterion), it covers the full attack surface (UC1-UC8). Methodology and tooling come from [Sprint 1 Testing and Validation](../../Sprint1/TestingAndValidation/testingAndValidation.md); this report focuses on findings and per-use-case verdicts. Container and deployment-chain controls are documented in [Security Configuration and Installation](../SecurityConfigurationAndInstallation/securityConfigurationAndInstallation.md).

## Table of Contents

1. [Assessment Scope](#1-assessment-scope)
2. [Methodology](#2-methodology)
3. [Per-Use-Case Probes](#3-per-use-case-probes)
4. [Vulnerability Management](#4-vulnerability-management)
5. [Risk Evaluation](#5-risk-evaluation)
6. [Monitoring Considerations](#6-monitoring-considerations)
7. [Assessment Outcome](#7-assessment-outcome)

---

## 1. Assessment Scope

| Asset | Use case(s) | Sprint introduced | Re-assessed in Sprint 2 |
|---|---|---|---|
| Auth0 JWT validation + role guard | UC1 | Sprint 1 | Yes (unchanged) |
| `MovieController` (read) | UC2 | Sprint 1 | Yes (unchanged) |
| `OrderController` + `ReceiptFileService` | UC3 | Sprint 1 | Yes (unchanged) |
| `RefundController` (create) | UC4 | Sprint 1 | Yes (unchanged) |
| `RefundController` (list) | UC5 | Sprint 2 | Yes (new) |
| `RefundController` (approve/reject) | UC6 | Sprint 2 | Yes (new) |
| `MovieController` (write) | UC7 | Sprint 2 | Yes (new) |
| `UserController` (role admin) | UC8 | Sprint 2 | Yes (new) |
| `TokenFreshnessFilter` + `TokenInvalidationService` | UC8 | Sprint 2 | Yes (new) |
| `Auth0ManagementClient.invalidateSessions` | UC8 | Sprint 2 | Yes (new) |
| Deployment chain (GitHub Actions -> VM -> Docker) | n/a | Sprint 2 | Yes (new) |

Out of scope: Auth0 tenant configuration (managed externally) and VM hardening below the Docker layer.

---

## 2. Methodology

The assessment combined four complementary techniques:

1. **SAST (CodeQL).** Re-run on each push and PR; results published to the GitHub Security tab.
2. **SCA (OWASP Dependency-Check + Anchore SBOM).** Re-evaluated the five accepted CVEs from Sprint 1.
3. **DAST (OWASP ZAP API scan).** Authenticated OpenAPI-driven scan against the ephemeral CI environment.
4. **Targeted security scenarios.** Hand-written cases per use case (Cap3) that ZAP cannot reach on its own (multi-actor flows, stateful denylist, ownership checks).

Each technique remains automated in the security pipeline ([Pipeline Automation Cap2](../PipelineAutomation/pipelineAutomation.md#2-security-pipeline-updates)).

---

## 3. Per-Use-Case Probes

One row per relevant attack scenario, grouped by use case. Each row was executed by either:

- a Spring `@SpringBootTest` integration test under `App/src/test/java/.../controller/*IntegrationTests.java` (default), or
- a request from the [Postman collection](../../../../Postman/EMovieShop_DESOFS.postman_collection.json) when manual reproduction was useful.

All scenarios passed.

### 3.1 UC1 - Login (Auth0 delegated)

| # | Scenario | Expected control | Observed | Evidence |
|---|---|---|---|---|
| UC1.a | Anonymous request to any `/api/**` | Spring Resource Server returns 401 | 401 with `WWW-Authenticate: Bearer` | [`SecurityConfigIntegrationTest`](../../../../App/src/test/java/com/example/desofs/config/SecurityConfigIntegrationTest.java) |
| UC1.b | Tampered JWT signature | `JwtDecoder` rejects | 401 `invalid_token` | [`SecurityConfigIntegrationTest`](../../../../App/src/test/java/com/example/desofs/config/SecurityConfigIntegrationTest.java) |
| UC1.c | JWT with wrong `aud` claim | `AudienceValidator` rejects | 401 `invalid_token`; reason logged | [`AudienceValidatorTest`](../../../../App/src/test/java/com/example/desofs/config/AudienceValidatorTest.java) |
| UC1.d | JWT without `https://emovieshop/roles` claim | `Auth0RolesConverter` returns empty authorities, RoleGuard 403 | 403 | [`SecurityConfigIntegrationTest`](../../../../App/src/test/java/com/example/desofs/config/SecurityConfigIntegrationTest.java) |

### 3.2 UC2 - Browse Available Movies

| # | Scenario | Expected control | Observed | Evidence |
|---|---|---|---|---|
| UC2.a | `CUSTOMER` calls `GET /api/movies/{id}` with valid id | 200 with DTO | 200, no internal fields leaked | [`MovieControllerIntegrationTests`](../../../../App/src/test/java/com/example/desofs/controller/MovieControllerIntegrationTests.java) |
| UC2.b | `CUSTOMER` calls `GET /api/movies` (admin-only listing) | 403 | 403 | [`MovieControllerIntegrationTests`](../../../../App/src/test/java/com/example/desofs/controller/MovieControllerIntegrationTests.java) |
| UC2.c | Path traversal `GET /api/movies/../actuator/env` | Spring routing returns 404 | 404; ZAP rule `6` suppressed as numeric-id false-positive | DAST (ZAP report) |

### 3.3 UC3 - Purchase Movie

| # | Scenario | Expected control | Observed | Evidence |
|---|---|---|---|---|
| UC3.a | `CUSTOMER` posts a valid order | 201 + receipt PDF written | 201; receipt file owned by container user `emovieshop` | [`OrderControllerIntegrationTests`](../../../../App/src/test/java/com/example/desofs/controller/OrderControllerIntegrationTests.java) |
| UC3.b | Negative quantity in payload | `@Valid` Bean Validation rejects | 400 with field-level error from `GlobalExceptionHandler` | [`InputValidationSecurityTest`](../../../../App/src/test/java/com/example/desofs/controller/InputValidationSecurityTest.java) |
| UC3.c | Receipt name with `../etc/passwd` and embedded `\0` | `ReceiptFileService` rejects after sanitization | 400 `Invalid request`; correlation id logged | [`InputValidationSecurityTest`](../../../../App/src/test/java/com/example/desofs/controller/InputValidationSecurityTest.java) |
| UC3.d | Customer A reads Customer B's receipt | Ownership check rejects | 403 | [`OrderControllerIntegrationTests`](../../../../App/src/test/java/com/example/desofs/controller/OrderControllerIntegrationTests.java) |

### 3.4 UC4 - Request Refund

| # | Scenario | Expected control | Observed | Evidence |
|---|---|---|---|---|
| UC4.a | `CUSTOMER` requests refund of own order | 201 with `PENDING` state | 201 | [`RefundControllerIntegrationTests`](../../../../App/src/test/java/com/example/desofs/controller/RefundControllerIntegrationTests.java) |
| UC4.b | `CUSTOMER` requests refund on someone else's order | 403 | 403 | [`RefundControllerIntegrationTests`](../../../../App/src/test/java/com/example/desofs/controller/RefundControllerIntegrationTests.java) |
| UC4.c | Burst above the configured per-IP / per-user rate (300 / 120 req/min) on the create endpoint | `RateLimitFilter` returns 429 | 429 with `Retry-After` | [`RefundControllerIntegrationTests`](../../../../App/src/test/java/com/example/desofs/controller/RefundControllerIntegrationTests.java) (rate-limit case) |

### 3.5 UC5 - View Refund Requests (Sprint 2)

| # | Scenario | Expected control | Observed | Evidence |
|---|---|---|---|---|
| UC5.a | `SUPPORT` calls `GET /api/refunds` | 200 with paginated list | 200 | [`RefundControllerIntegrationTests`](../../../../App/src/test/java/com/example/desofs/controller/RefundControllerIntegrationTests.java) |
| UC5.b | `CUSTOMER` calls `GET /api/refunds` | RoleGuard returns 403 | 403, no body leak | [`RefundControllerIntegrationTests`](../../../../App/src/test/java/com/example/desofs/controller/RefundControllerIntegrationTests.java) |
| UC5.c | SQL injection probe via `?status=PENDING' OR 1=1--` | JPA parameter binding neutralises | 400 `Invalid request` (enum coercion fails); no DB error leaked | DAST (ZAP report) + [`InputValidationSecurityTest`](../../../../App/src/test/java/com/example/desofs/controller/InputValidationSecurityTest.java) |
| UC5.d | `SUPPORT` calls `GET /api/refunds/{id}` | 200 with detail DTO when found, 404 otherwise | 200 / 404 | [`RefundControllerIntegrationTests`](../../../../App/src/test/java/com/example/desofs/controller/RefundControllerIntegrationTests.java) |
| UC5.e | `CUSTOMER` calls `GET /api/refunds/{id}` | RoleGuard returns 403 | 403 | [`RefundControllerIntegrationTests`](../../../../App/src/test/java/com/example/desofs/controller/RefundControllerIntegrationTests.java) |

### 3.6 UC6 - Handle Refund Request

| # | Scenario | Expected control | Observed | Evidence |
|---|---|---|---|---|
| UC6.a | `SUPPORT` approves a `PENDING` refund | State machine -> `APPROVED`; audit log entry | 200; row visible in `audit_log` | [`RefundControllerIntegrationTests`](../../../../App/src/test/java/com/example/desofs/controller/RefundControllerIntegrationTests.java), [`AuditLogControllerIntegrationTests`](../../../../App/src/test/java/com/example/desofs/controller/AuditLogControllerIntegrationTests.java) |
| UC6.b | `SUPPORT` approves an already-`APPROVED` refund | Domain rule throws | 400 with deterministic message | [`RefundControllerIntegrationTests`](../../../../App/src/test/java/com/example/desofs/controller/RefundControllerIntegrationTests.java) |
| UC6.c | `CUSTOMER` calls `POST /api/refunds/{id}/approve` | RoleGuard returns 403 | 403 | [`RefundControllerIntegrationTests`](../../../../App/src/test/java/com/example/desofs/controller/RefundControllerIntegrationTests.java) |

### 3.7 UC7 - Manage Movie Catalog (Sprint 2)

| # | Scenario | Expected control | Observed | Evidence |
|---|---|---|---|---|
| UC7.a | `ADMIN` posts a valid movie | 201 with new id | 201; `audit_log` row written | [`MovieControllerIntegrationTests`](../../../../App/src/test/java/com/example/desofs/controller/MovieControllerIntegrationTests.java) |
| UC7.b | `SUPPORT` posts a movie | 403 | 403 | [`MovieControllerIntegrationTests`](../../../../App/src/test/java/com/example/desofs/controller/MovieControllerIntegrationTests.java) |
| UC7.c | XSS payload in `title` (`<script>alert(1)</script>`) | Stored as-is; output is JSON, no HTML rendering on the API side | 201; payload returned escaped in JSON, no HTML execution path | [`InputValidationSecurityTest`](../../../../App/src/test/java/com/example/desofs/controller/InputValidationSecurityTest.java) |
| UC7.d | Title exceeding `@Size(max=255)` | Bean Validation rejects | 400 with field-level error | [`MovieControllerIntegrationTests`](../../../../App/src/test/java/com/example/desofs/controller/MovieControllerIntegrationTests.java) |
| UC7.e | `ADMIN` calls `DELETE /api/movies/{id}` | 204 No Content; `DELETE_MOVIE` audit entry written | 204; `audit_log` row visible | [`MovieControllerIntegrationTests`](../../../../App/src/test/java/com/example/desofs/controller/MovieControllerIntegrationTests.java) |
| UC7.f | `CUSTOMER` calls `DELETE /api/movies/{id}` | RoleGuard returns 403 | 403, no body leak | [`MovieControllerIntegrationTests`](../../../../App/src/test/java/com/example/desofs/controller/MovieControllerIntegrationTests.java) |

### 3.8 UC8 - Manage Roles (Sprint 2)

| # | Scenario | Expected control | Observed | Evidence |
|---|---|---|---|---|
| UC8.a | Anonymous request to `POST /api/users/{id}/roles` | 401 | 401 `WWW-Authenticate: Bearer` | [`UserControllerIntegrationTests`](../../../../App/src/test/java/com/example/desofs/controller/UserControllerIntegrationTests.java) |
| UC8.b | `ADMIN` calls `POST /api/users/{adminSelf}/roles` | `UserService.guardSelfModification` throws | 400 from `GlobalExceptionHandler` | [`UserControllerIntegrationTests`](../../../../App/src/test/java/com/example/desofs/controller/UserControllerIntegrationTests.java) |
| UC8.c | Admin removes `ADMIN` role from a target user; that user replays a token issued before the cut-off | `TokenFreshnessFilter` rejects | 401 `invalid_token`; `iat` denylist hit logged | [`TokenFreshnessFilterTest`](../../../../App/src/test/java/com/example/desofs/security/TokenFreshnessFilterTest.java), [`TokenInvalidationServiceTest`](../../../../App/src/test/java/com/example/desofs/services/TokenInvalidationServiceTest.java) |
| UC8.d | Same as UC8.c but using a refreshed token issued after the cut-off | Token must be accepted | 200 returned for permitted endpoints | [`TokenFreshnessFilterTest`](../../../../App/src/test/java/com/example/desofs/security/TokenFreshnessFilterTest.java) |
| UC8.e | Auth0 Management API forced offline during role change | DB cut-off persists, Auth0 call swallowed | DB write committed, WARN log emitted, request returns 204 | [`Auth0ManagementClientTest`](../../../../App/src/test/java/com/example/desofs/security/Auth0ManagementClientTest.java) |

---

## 4. Vulnerability Management

The Sprint 2 dependency tree was re-scanned against the same NVD/CISA feeds used in Sprint 1 (Dependency-Check 11.1.1, data 2026-06-11). The reported set is identical to Sprint 1.

| | Sprint 1 | Sprint 2 |
|---|---|---|
| Reported CVEs | 5 | 5 |
| Critical / High exploitable | 0 | 0 |
| Build-breaking (CVSS >= 9) | 0 | 0 |

The five accepted CVEs documented in [Sprint 1 Cap4.2](../../Sprint1/TestingAndValidation/testingAndValidation.md#sca-results-dependency-check) (`angus-activation`, `hibernate-validator`, three `swagger-ui`/DOMPurify entries) were re-checked. Status:

| Dependency | CVE | Action this sprint |
|---|---|---|
| angus-activation 2.0.3 | CVE-2025-7962 | No upstream fix; mitigations stand. Re-check next sprint. |
| hibernate-validator 8.0.3 | CVE-2025-15104 | No upstream fix compatible with Spring Boot 3.5; mitigations stand. |
| swagger-ui 5.32.2 / DOMPurify 3.3.2 | CVE-2026-41238 / -41239 / -41240 | Swagger UI remains disabled in production; mitigations stand. |

---

## 5. Risk Evaluation

| Residual risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Auth0 Management API outage during a role change | Low | Medium | Local denylist is authoritative; Auth0 call is best-effort. WARN log surfaces the failure for follow-up. |
| GitHub Secret leakage by misconfigured workflow | Low | High | Secrets are only consumed in actions, never echoed. Tokens are masked. PR-based workflow review catches accidental dumps. |
| Accepted CVEs become exploitable upstream | Low | Medium | Mitigations documented per CVE; re-scan each sprint. |
| Production MySQL reached on the VM loopback shares the host network namespace with the database server | Low | Medium | Only port 8080 is exposed by the container; the host firewall restricts inbound traffic and the MySQL port is not published outside the VM. |
| Rate limit counters (Bucket4j) are kept in memory and reset whenever the container restarts | Low | Low | Restarts are infrequent and only briefly relax the limit. A distributed store (e.g. Redis) would be needed if the app is ever scaled out. |

---

## 6. Monitoring Considerations

The Sprint 2 deployment is API-only and not the focus of the project, so monitoring stays lean:

- **Application logs.** Captured in `runtime_execution.log` during CI; in production, logs flow to the container's stdout and are retained by the Docker logging driver.
- **Audit log.** `AuditLogService` records role changes (UC8), refund lifecycle (UC4-UC6), movie creation (UC7), and order creation (UC3). Restricted to `ADMIN` via `GET /api/audit-logs`.
- **Health endpoint.** `/actuator/health` is publicly accessible and used by the container healthcheck and the deploy job.
- **Pipeline alerting.** CodeQL alerts surface in the GitHub Security tab; ZAP failures break the build and require triage before the next release.

Items intentionally out of scope (acknowledged in the rubric for the Sprint 2 *Production* / *Operate* criteria, weighted at 5% each): centralised log aggregation, SIEM integration, automated incident response, scheduled backups beyond MySQL's default `mysqldump` capability.

---

## 7. Assessment Outcome

| | |
|---|---|
| Sprint | Sprint 2 |
| Use cases assessed | UC1-UC8 (all) |
| Overall result | Pass |
| Critical / High findings | 0 |
| Medium findings | 0 |
| Low findings | 1 (root-path Content-Type, suppressed in `rules.tsv` since Sprint 1) |
| Accepted CVEs | 5 (same set as Sprint 1) |
| Build-breaking issues | 0 |

The pipeline policy (`fail_action: true`, CVSS >= 9 build break, fail on CodeQL High) remained blocking and stayed green throughout Sprint 2.
