# Testing and Validation - Phase 2, Sprint 2

This document records the **Sprint 2 deltas** on top of the testing strategy established in [Sprint 1 Testing and Validation](../../Sprint1/TestingAndValidation/testingAndValidation.md). Tooling (CodeQL, OWASP Dependency-Check, OWASP ZAP, runtime log analysis as IAST alternative) is unchanged.

## Index

- [1. Sprint 2 Scope](#1-sprint-2-scope)
- [2. New and Updated Tests](#2-new-and-updated-tests)
- [3. Coverage](#3-coverage)
- [4. Security Testing Results](#4-security-testing-results)
- [5. Results & Observations](#5-results--observations)

---

## 1. Sprint 2 Scope

The Sprint 2 test effort focuses on the new code introduced for UC5-UC8 and the deployment surface:

- UC5 / UC7 list and CRUD endpoints on `RefundController` and `MovieController`.
- UC8 server-side token invalidation (`TokenInvalidationService`, `TokenFreshnessFilter`) and Auth0 session revocation (`Auth0ManagementClient.invalidateSessions`).
- Movie domain rules (`MovieTest`) and service behaviour (`MovieServiceTests`).
- Containerised production runtime (smoke check on `/actuator/health`).

---

## 2. New and Updated Tests

| Test class | Type | What it covers |
|---|---|---|
| [`MovieTest`](../../../../App/src/test/java/com/example/desofs/domain/MovieTest.java) | Unit | Movie domain invariants and constructors |
| [`MovieServiceTests`](../../../../App/src/test/java/com/example/desofs/services/MovieServiceTests.java) | Unit | `MovieService` create/get/list/update branches |
| [`TokenInvalidationServiceTest`](../../../../App/src/test/java/com/example/desofs/services/TokenInvalidationServiceTest.java) | Unit | Cut-off persistence, `isTokenInvalidated` decision logic |
| [`TokenFreshnessFilterTest`](../../../../App/src/test/java/com/example/desofs/security/TokenFreshnessFilterTest.java) | Unit | 401 on stale `iat`; pass-through for missing claims; pass-through for non-JWT requests |
| [`Auth0ManagementClientTest`](../../../../App/src/test/java/com/example/desofs/security/Auth0ManagementClientTest.java) | Unit | `invalidateSessions_success`, `invalidateSessions_swallowsAuth0Errors`, `invalidateSessions_rejectsInvalidUserId` |
| [`MovieControllerIntegrationTests`](../../../../App/src/test/java/com/example/desofs/controller/MovieControllerIntegrationTests.java) | Integration | UC7: list / get / create / update including RBAC and `@Valid` failures |
| [`RefundControllerIntegrationTests`](../../../../App/src/test/java/com/example/desofs/controller/RefundControllerIntegrationTests.java) | Integration | UC5 listing as `SUPPORT`, UC4 creation as `CUSTOMER`, approval / rejection flows |
| [`UserControllerIntegrationTests`](../../../../App/src/test/java/com/example/desofs/controller/UserControllerIntegrationTests.java) | Integration | UC8 list users / assign role / remove role; self-modification guard |

---

## 3. Coverage

JaCoCo report (`App/target/site/jacoco/index.html`) after Sprint 2:

| Package | Coverage |
|---|---|
| `com.example.desofs.config` | 100% |
| `com.example.desofs.controllers` | 100% |
| `com.example.desofs.domain` | 100% |
| `com.example.desofs.exceptions` | 96% |
| `com.example.desofs.security` | 93% |
| `com.example.desofs.services` | 95% |
| `com.example.desofs.shared.mappers` | 100% |
| Total | 96% |

The CI quality gate (>= 70%) remains green.

---

## 4. Security Testing Results

### 4.1 SAST - CodeQL

| Metric | Result |
|---|---|
| Critical / High findings | 0 |
| Build-breaking findings | 0 |
| Build status | Pass |

The new UC8 code (`TokenFreshnessFilter`, `Auth0ManagementClient`) was scanned with no new alerts.

### 4.2 SCA - OWASP Dependency-Check

OWASP Dependency-Check 11.1.1 (NVD data 2026-06-11) was re-run against the Sprint 2 dependency tree. Compared with Sprint 1, the only change is in the transitive Spring Framework version (6.2.19 instead of 6.2.18); the five Sprint 1 CVEs without an upstream fix remain on the accepted-risk list with the original mitigations.

| Metric | Sprint 1 | Sprint 2 |
|---|---|---|
| Total dependencies scanned | ~50+ transitive | ~50+ transitive |
| Vulnerabilities reported | 5 (accepted) | 5 (accepted) |
| Build-breaking threshold (CVSS >= 9) | Not exceeded | Not exceeded |
| Build status | Pass | Pass |

The five accepted CVEs documented in [Sprint 1 Cap4.2](../../Sprint1/TestingAndValidation/testingAndValidation.md#sca-results-dependency-check) (`angus-activation`, `hibernate-validator`, three `swagger-ui`/DOMPurify entries) were re-evaluated; no upstream fix has shipped between Sprints. The mitigations recorded in Sprint 1 (Swagger UI disabled in production, validated input via DTOs, stable Spring Boot pin) remain in place.

### 4.3 DAST - OWASP ZAP

The OpenAPI spec now exposes the UC5/UC7/UC8 endpoints. The same OpenAPI-driven scan was re-run, authenticated with the M2M Bearer token via the ZAP Replacer (see [Pipeline Automation Cap2](../PipelineAutomation/pipelineAutomation.md#2-security-pipeline-updates)).

| Risk Level | Sprint 1 | Sprint 2 |
|---|---|---|
| High | 0 | 0 |
| Medium | 0 | 0 |
| Low | 1 | 1 |
| Informational | 3 | 3 |

The single Low alert is the same root-path Content-Type finding observed in Sprint 1 (suppressed in `rules.tsv` with plugin id `100001`). The three Informational categories (`Client Error response codes`, `Non-Storable Content`, `User Agent Fuzzer`) are unchanged in nature; the absolute count of client-error responses grew slightly because the new UC5/UC7/UC8 endpoints provide additional surface for ZAP's malformed-payload probes - all of which were correctly rejected with `400` / `401` / `403` / `404` / `429`. The new UC8 endpoints (`POST /api/users/{id}/roles`, `DELETE /api/users/{id}/roles`) returned `401` for unauthenticated probes and `403` for non-admin tokens, with no leaked error details.

### 4.4 IAST Alternative - Runtime Log Analysis

The runtime log analysis was re-run during the DAST step. New observations specific to Sprint 2:

- **Token denylist enforcement.** When the test suite revoked a role mid-flow, subsequent requests were rejected by `TokenFreshnessFilter` with `401 invalid_token`. The corresponding log line `"Rejected request: token issued before invalidation cutoff for the subject"` (`TokenFreshnessFilter`) was observed without leaking subject identifiers in client-facing responses.
- **Auth0 best-effort failure handling.** Forced failures of the Auth0 Management API surfaced as WARN log entries from `Auth0ManagementClient.invalidateSessions` without rolling back the local denylist write, matching the design intent.
- **Self-modification guard.** Attempts to call `assignRole` / `removeRole` with `actorId == targetUserId` produced `400 Bad Request` and the rejection was visible in the log.

No critical errors were detected in `runtime_execution.log` for Sprint 2.

---

## 5. Results & Observations

### Overall Status

| | |
|---|---|
| **Date** | 2026-06-11 |
| **Sprint** | Sprint 2 |
| **Overall result** | Pass |

### Issues Identified

| Severity | Count | Detail |
|---|---|---|
| Critical | 0 | - |
| High | 0 | - |
| Medium | 0 | - |
| Low | 1 | Same root-path Content-Type finding as Sprint 1 - accepted false positive, suppressed in `rules.tsv` |
| Accepted CVEs | 5 | Same set as Sprint 1; status unchanged |

### Recommendations

- Re-scan SCA dependencies next sprint as upstream fixes for the five accepted CVEs (`angus-activation`, `hibernate-validator`, `swagger-ui`/DOMPurify) may become available.
- Maintain the current `fail_action: true` ZAP policy and the CVSS >= 9 build-break threshold on Dependency-Check - the pipeline is clean and should remain blocking on any new High or Medium finding.

The pipeline policy (`fail_action: true` on ZAP, CVSS >= 9 on Dependency-Check, fail-on-error on CodeQL) remains blocking and stayed green throughout Sprint 2.
