# Testing and Validation

## Index

- [1. Strategy Overview](#2-strategy-overview)
- [2. Testing Approach](#3-testing-approach)
    - [2.1 SAST - Static Analysis](#31-sast---static-analysis)
    - [2.2 SCA - Dependency Scanning](#32-sca---dependency-scanning)
    - [2.3 DAST - Dynamic Testing](#34-dast---dynamic-testing)
    - [2.4 IAST - Instrumented Testing](#35-iast---instrumented-testing)
- [3. Functional Tests](#4-functional-tests)
- [4. Security Testing Results](#5-security-testing-results)
- [5. Results & Observations](#6-results--observations)
- [6. Tool Selection Justification Summary](#7-tool-selection-justification-summary)
- [7. References](#8-references)

Here we document the test strategy, executed tests, and validation results for Sprint 1. This strategy integrates security testing throughout the development lifecycle (SAST, DAST, IAST, and SCA).

## 1. Strategy Overview

The testing strategy follows three core principles:

- **Early Detection**: Security testing integrated from the start
- **Automation**: Continuous security scanning in the CI/CD pipeline
- **Multiple Layers**: Different testing methodologies catch vulnerabilities at different stages

## 2. Testing Approach

### 2.1 SAST - Static Analysis

Catches vulnerabilities before compilation.

| Aspect | CodeQL | SonarQube | Checkmarx |
|--------|--------|-----------|----------|
| Cost | Free (GitHub) | Free/Paid | Expensive |
| Java Support | Excellent | Excellent | Excellent |
| Setup Complexity | Simple | Simple | Complex |
| CI/CD Integration | Native GitHub | Good | Good |
| Centralized Results | GitHub Security tab | External server | External server |

**Selected: CodeQL**
- Free and natively integrated with GitHub Actions
- Findings published directly to the **GitHub Security tab**, no external server or credentials required
- Excellent Java/Spring Boot support (detects injection, path traversal, unsafe deserialization, crypto issues)
- Runs on every push and PR, blocking merges when vulnerabilities are found
- Keeps SAST results, PR checks, and vulnerability alerts in one centralized place

> **Note:** CodeQL was chosen over SonarQube because it integrates natively with GitHub, findings are published directly to the **GitHub Security tab**, keeping static analysis results, PR checks, and vulnerability alerts in one centralized place without requiring an external server or additional credentials.

#### Local SAST (pre-push)

In addition to CodeQL in the CI pipeline, two Maven plugins run **locally during the build** to catch bugs before code is even pushed to the repository:

| Plugin | Purpose |
|--------|---------|
| **SpotBugs + FindSecBugs** | Detects common bug patterns and security-specific issues (SQL injection, XSS, path traversal, weak crypto) at bytecode level |
| **PMD** | Identifies code smells, dead code, and potential bugs via source-level static analysis rules |

Both plugins are configured in `pom.xml` and execute during `mvn verify`, so we can get immediate feedback on our local machine before creating a PR.

### 2.2 SCA - Dependency Scanning

Third-party libraries often contain known vulnerabilities.

| Tool | Speed | Accuracy | Cost | Integration |
|------|-------|----------|------|-------------|
| OWASP Dependency-Check | Fast | Good | Free | Maven plugin |
| Snyk | Very Fast | Excellent | Freemium | GitHub/GitLab |
| Black Duck | Slow | Excellent | Expensive | Complex |

**Selected: OWASP Dependency-Check**
- Free and open-source
- Integrates directly into Maven build (`pom.xml`)
- Generates Software Bill of Materials (SBOM)
- No external accounts required

### 2.3 DAST - Dynamic Testing

Tests the running application as an attacker would (black box).

| Tool | Ease of Use | Accuracy | Cost | Best For |
|------|-------------|----------|------|----------|
| OWASP ZAP | Easy | Good | Free | Quick scans, continuous integration |
| Burp Suite | Easy | Excellent | Free/Paid | Comprehensive testing, professional |
| Acunetix | Medium | Excellent | Expensive | Enterprise needs |

**Selected: OWASP ZAP**
- Free and open-source
- Active community and regular updates
- Excellent for CI/CD integration
- No licensing concerns

### 2.4 IAST - Instrumented Testing

IAST instruments the application at runtime (via a JVM agent) and observes code execution paths as requests flow through, combining the depth of SAST with the realism of DAST. Unlike DAST (black box), IAST can pinpoint the exact vulnerable line of code.

| Tool | License | JVM Agent | Notes |
|------|---------|-----------|-------|
| Contrast Security | Commercial | Yes | Enterprise-grade, real-time feedback |
| Seeker (Synopsys) | Commercial | Yes | CI/CD integration |
| HCL AppScan | Commercial | Yes | Broad language support |

The adoption of IAST tests for Java applications is heavily constrained by the commercial nature of the available tools. Contrast Security previously offered a Community Edition that provided some IAST capabilities free of charge, however, this offering was discontinued in early 2025, leaving no tool, free or open-source, available for Java/Spring Boot applications at the time of this project.

Given these constraints, integrating a IAST agent into the CI/CD pipeline was not possible without incurring licensing costs or relying on trial accounts with restricted functionality.

As alternative, we implemented a **runtime execution log analysis** step in the security pipeline. This approach starts the application with production-equivalent configuration, exercises it through the 
existing integration test suite, and captures the complete runtime output. The resulting log is then automatically scanned for security-relevant patterns before being archived as a pipeline artifact for 
manual review.

While this does not replicate the data-flow instrumentation that a true IAST agent provides, it achieves partial coverage of the same intent like detecting runtime security anomalies that are only observable when the application is executing under realistic conditions, and that would not be caught by static analysis alone.

While we are aware that this is not a complete implementation of the IAST methodology, the adopted approach is a pragmatic substitute constrained by tool availability.
---

## 4. Functional Tests

The functional testing strategy for this sprint was split into two layers: unit tests and integration tests. The goal was to validate the business rules in isolation first, and then verify that the main application flows work correctly when the Spring context, persistence layer, security configuration, and web layer are exercised together.

### 4.1 Unit Tests

Unit tests focus on the smallest verifiable units of behaviour. In this project, they cover:

- **Domain classes**: validation rules, invariants, state transitions, and helper methods in the core model.
- **Mappers**: conversion between entities, DTOs, and domain objects, ensuring field mapping and transformation logic are correct.
- **Services**: business rules, branching logic, and exception handling.

To keep unit tests fast and deterministic, external dependencies are replaced with **mocks**. Repository access, file I/O, time-sensitive operations, and other collaborators are mocked so each test only exercises the code under test. This makes it possible to isolate failure causes and validate expected behaviour with both positive and negative scenarios.

The unit test style combines:

- **White-box testing** for service methods and domain logic, because the test cases are derived from the internal control flow, validation branches, and exception paths.
- **Black-box testing** for mappers and public domain behaviour, because the focus is on observable input/output rather than implementation details.

### 4.2 Integration Tests

Integration tests verify that multiple application layers work correctly together. Here the emphasis is on the interaction between:

- **Controllers** and request/response handling
- **Services** and their collaborators
- **Domain objects** and persistence mappings
- **Security and validation layers** where applicable

These tests run with a larger portion of the Spring Boot stack enabled, so they validate routing, serialization, validation, dependency injection, and the end-to-end behaviour of the main API flows. Compared with unit tests, they are broader and slower, but they provide stronger confidence that the application behaves correctly in realistic execution paths.

### 4.3 Coverage Tracking

The obtained code coverage is reported per package and globally:

| Package | Coverage Status |
|---------|-----------------|
| `com.example.desofs.services` | 92% |
| `com.example.desofs.controllers` | 88% |
| `com.example.desofs.security` | 92% |
| `com.example.desofs.config` | 95% |
| `com.example.desofs.exceptions` | 97% |
| `com.example.desofs.domain` | 100% |
| `com.example.desofs.shared.mappers` | 100% |
| `com.example.desofs` | 37% |
| Total | 94% |

### 4.4 Token Invalidation & Session Revocation Tests

The token-invalidation and session-revocation feature documented in [Development §12](../Development/development.md#12-token-invalidation--session-revocation-on-role-change) is exercised end-to-end by a dedicated set of unit tests. The full backend test suite (316/316) is green.

| Test class | Tests | What is verified |
|---|---|---|
| `App/src/test/java/com/example/desofs/services/TokenInvalidationServiceTest.java` | 10 | Insert when no row exists; refresh existing cut-off (idempotent upsert); `iat` strictly before / equal to / after cut-off; no entry → returns `false`; null `userId` / null `iat` → `false` without DB hit; blank `userId` → `IllegalArgumentException`; blank `reason` normalised to `"UNSPECIFIED"`; `getInvalidatedAfter` returns `Optional.empty()` and the stored timestamp. AssertJ `within(...)` is used to assert the persisted timestamp is close to "now". |
| `App/src/test/java/com/example/desofs/security/TokenFreshnessFilterTest.java` | 5 | Unauthenticated request → pass-through; non-JWT auth → pass-through; fresh JWT (`iat ≥ cutoff`) → pass-through; stale JWT (`iat < cutoff`) → 401 with `WWW-Authenticate: Bearer error="invalid_token"` and JSON body; missing `iat` claim → pass-through (no false rejection). |
| `App/src/test/java/com/example/desofs/security/Auth0ManagementClientTest.java` | +3 (delta) | `invalidateSessions` issues `DELETE /api/v2/users/{id}/sessions` with the Management-API bearer token; HTTP 500 from Auth0 is swallowed (logged at WARN, no exception); blank/null `userId` is rejected before any network call. |
| `App/src/test/java/com/example/desofs/services/UserServiceTest.java` | +N (delta) | `ITokenInvalidationService` is mocked in addition to the existing `IAuth0ManagementClient` and `IAuditLogService` mocks; `assignRole` and `removeRole` both call `invalidateTokensFor(...)` **after** the audit log and **together with** `auth0.invalidateSessions(...)`, with the correct `ROLE_ASSIGNED:<ROLE>` / `ROLE_REMOVED:<ROLE>` reason. Call order is enforced with `InOrder`. |
| Controller integration slices | 4 files | `AuditLogControllerIntegrationTests`, `MovieControllerIntegrationTests`, `OrderControllerIntegrationTests` and `RefundControllerIntegrationTests` declare `@MockitoBean ITokenInvalidationService` so that `@WebMvcTest` can wire `TokenFreshnessFilter` without bringing up JPA. This guarantees the new filter sits in every controller slice's request pipeline exactly as in production. |

#### Why these tests, mapped to the threat model

- **Cut-off semantics (`TokenInvalidationServiceTest`)** - directly validate the ASVS V7.4.1 control: a JWT issued *before* the per-user cut-off must be considered revoked. The before/equal/after-cut-off cases pin the comparison to "strictly before", preventing both a one-second false-positive at issuance and a one-second false-negative after revocation.
- **Filter rejection contract (`TokenFreshnessFilterTest`)** - the 401 + `WWW-Authenticate` shape is asserted byte-for-byte so the SPA's existing 401 handler keeps working and clients are forced to re-authenticate (no soft-fail).
- **Null-tolerance** - both the service and the filter are tested with null inputs because the JWT `iat` claim is optional in the OAuth2 spec; a `NullPointerException` here would convert a benign request into a 500 and could be used as an availability probe.
- **Auth0 best-effort (`Auth0ManagementClientTest`)** - verifies that an IdP outage cannot roll back the local database transaction (defence-in-depth: the denylist is the authoritative control, the Auth0 call only improves UX).
- **Call-order in `UserServiceTest`** - ensures the denylist write happens *after* the audit-log entry (so the action is auditable even if the Auth0 call later fails) and *before* the request returns (so no further requests from the affected user can succeed against the same JWT).

---

## 5. Security Testing Results


### SAST Results (CodeQL)

No vulnerabilities were identified by CodeQL in this sprint. The analysis 
covered the full Java codebase and results are published to the GitHub Security 
tab, reviewed on every push and pull request.

| Metric | Result |
|--------|--------|
| Critical / High findings | 0 |
| Build-breaking findings | 0 |
| Build status | Pass |

---

### SCA Results (Dependency-Check)

| Metric | Status |
|--------|--------|
| Total dependencies scanned | ~50+ transitive |
| Vulnerabilities remaining | 5 CVEs (accepted risk) |
| Build-breaking threshold | CVSS ≥ 9 |
| Build status | Pass |

#### Accepted Risk - Unfixable CVEs

The following CVEs have no available fix at the time of this sprint. They are documented as **accepted risk** with justification:

| Dependency | CVE | CVSS | Risk Justification |
|---|---|---|---|
| angus-activation 2.0.3 | CVE-2025-7962 | Low-Medium | No newer version available; managed by Spring Boot 3.5.14. Email activation library with minimal attack surface in our REST API context. |
| hibernate-validator 8.0.3 | CVE-2025-15104 | Medium | Latest 8.x release; version 9.x is incompatible with Spring Boot 3.5. Requires specific usage patterns not present in our codebase. |
| swagger-ui 5.32.2 (DOMPurify 3.3.2) | CVE-2026-41240 | Medium | Swagger UI is **disabled in production** (`springdoc.swagger-ui.enabled=false`). Only enabled in dev profile. The CVE requires using `ADD_TAGS` function + `FORBID_TAGS` simultaneously, a pattern Swagger UI does not use. |
| swagger-ui 5.32.2 (DOMPurify 3.3.2) | CVE-2026-41238 | Medium | Same as above, Swagger UI disabled in production; attack surface eliminated. |
| swagger-ui 5.32.2 (DOMPurify 3.3.2) | CVE-2026-41239 | Medium | Same as above, Swagger UI disabled in production; attack surface eliminated. |

**Mitigation controls in place:**
- Swagger UI and OpenAPI docs endpoints are disabled by default (`application.properties`)
- Only enabled via `application-dev.properties` in development profile
- Production deployments never expose the vulnerable Swagger UI frontend
- Dependencies will be re-scanned in future sprints as patches become available


### DAST Results (OWASP ZAP)

The ZAP API scan ran against the OpenAPI spec (`/v3/api-docs`) with an authenticated M2M JWT, covering all 254 documented endpoints across 119 active 
security checks.

| Risk Level | Findings |
|------------|----------|
| High | 0 |
| Medium | 0 |
| Low | 1 |
| Informational | 3 |


#### Low - Unexpected Content-Type (Plugin 100001)

Two requests probing paths outside the API contract returned `text/html` instead of `application/json`:

- `GET /?aaa=bbb`
- `GET /?class.module.classLoader.DefaultAssertionStatus=nonsense`

These target the root context path, which is not part of the API surface. The `text/html` response originates from Spring Boot's default error handler for 
unmapped routes. All defined `/api/**` endpoints return `application/json` exclusively, confirmed by the scan metric showing 94% of endpoints with 
`application/json` content type.

**Assessment:** False positive. Not exploitable. Suppressed in `rules.tsv` with plugin ID `100001`.

#### Informational Findings

| Finding | Assessment |
|---------|------------|
| Client Error response codes (285 instances) | Expected - the ZAP scanner sends malformed and attack payloads that are correctly rejected with `400`, `404`, and `429` responses. |
| Non-Storable Content | Expected for a stateless REST API returning dynamic JSON. |
| User Agent Fuzzer | Informational probe; no exploitable behaviour observed. |

#### IAST - Runtime Log Analysis Results

The runtime execution log captured during the DAST scan provides evidence of the application's behavior under active attack conditions.

**Input validation and type safety:** The ZAP scanner injected SQL injection payloads (e.g., WAITFOR DELAY, OR 1=1), OS command injection strings 
(e.g., `cat /etc/passwd`, ShellShock), and server-side template injection payloads (Freemarker, Velocity, Node.js) into typed fields. In all cases, 
Jackson's deserialization layer rejected the payloads before they reached the service or persistence layer, producing `400 Bad Request` responses with 
sanitized error messages containing only a correlation ID.

**Rate limiting:** The `RateLimitFilter` triggered repeatedly during the scan, confirming that both per-user and per-IP throttling are active and 
functioning under sustained attack traffic.

**Error handling:** No stack traces, internal class names, or sensitive system information were exposed in any error response. All exceptions were 
handled by the `GlobalExceptionHandler`, which returns stable HTTP status codes and generic messages.

**TLS probe rejection:** The scanner attempted TLS handshakes on the plain HTTP port. Tomcat rejected these requests at the protocol level with 
`Invalid character found in method name`, confirming no protocol confusion is possible.

No genuine security anomalies were identified in the runtime log. All flagged entries correspond to expected defensive behavior under adversarial 
input conditions.

---

## 6. Results & Observations

### Overall Status

| | |
|---|---|
| **Date** | 2026-05-16 |
| **Sprint** | Sprint 1 |
| **Overall result** | Pass |

### Issues Identified

| Severity | Count | Detail |
|----------|-------|--------|
| Critical | 0 | - |
| High | 0 | - |
| Medium | 0 | - |
| Low | 1 | Unexpected Content-Type on root path - accepted false positive |
| Accepted CVEs | 5 | No fix available; mitigations documented above |

### Recommendations

- Suppress plugin `100001` in `rules.tsv` to eliminate the root path 
  Content-Type finding in future scans, or configure Spring Boot's error 
  handling to return `application/json` for all unmapped routes.
- Re-scan SCA dependencies in Sprint 2 as patches for the accepted CVEs 
  may become available, particularly for `hibernate-validator`.
- Maintain the current `fail_action: true` ZAP policy - the pipeline is 
  clean and should remain blocking on any new High or Medium finding.



---

## 7. Tool Selection Justification Summary

| Tool | Purpose | Why Selected |
|------|---------|--------------|
| **CodeQL** | SAST | Free, native GitHub integration, centralized Security tab |
| **Dependency-Check** | SCA | Free, Maven plugin, no external accounts |
| **OWASP ZAP** | DAST | Free, CI/CD friendly, active community |
| **Maven** | Build/Test | Already in use, integrated security plugins |
| **Runtime Log Analysis** | IAST alternative | Alternative method due to tool availability |

---

## 8. References

### Testing Tools
- [CodeQL](https://codeql.github.com/) - GitHub-native SAST engine, results in GitHub Security tab
- [OWASP Dependency-Check](https://github.com/jeremylong/DependencyCheck) - Maven Plugin for SCA
- [OWASP ZAP](https://www.zaproxy.org/) - Automated DAST scanning tool

### Security Standards
- [OWASP Top 10](https://owasp.org/www-project-top-ten/) - Web application security risks
- [OWASP ASVS](https://owasp.org/www-project-application-security-verification-standard/) - Application Security Verification Standard
- [STRIDE Threat Modeling](https://en.wikipedia.org/wiki/STRIDE_(security)) - Threat analysis methodology
