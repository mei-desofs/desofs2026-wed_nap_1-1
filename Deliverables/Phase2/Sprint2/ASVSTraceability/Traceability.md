# ASVS Traceability - Phase 2, Sprint 2

This document is a **delta** to the Sprint 1 traceability table at [`../../Sprint1/ASVSTraceability/Traceability.md`](../../Sprint1/ASVSTraceability/Traceability.md). It records the ASVS 5.0.0 requirements whose status, implementation, or evidence changed in Sprint 2. Requirements not listed here keep their Sprint 1 status.

## Traceability Model

- ASVS requirement: ASVS 5.0.0 identifier and short statement.
- Implementation: where the control lives in the codebase.
- Evidence / tests: what validates the behaviour.
- Status: `Compliant`, `Partial`, `Not Applicable`.

## Index

- [V7 - Session Management](#v7---session-management)
- [V8 - Authorization](#v8---authorization)
- [V13 - Configuration](#v13---configuration)
- [V15 - Secure Coding and Architecture](#v15---secure-coding-and-architecture)
- [V16 - Security Logging and Error Handling](#v16---security-logging-and-error-handling)

---

## V7 - Session Management

### V7.4.1 - Disallow further use of a session on termination (with explicit support for self-contained tokens via "a list of terminated tokens" or "disallowing tokens produced before a per-user date and time")

- Sprint 1 status: `Partial` (Auth0-issued tokens were not invalidated server-side on role change).
- Sprint 2 status: `Compliant`.
- Implementation: `UserService.invalidateUserSessions` writes a per-user cut-off via `TokenInvalidationService` (the "per-user date and time" pattern explicitly allowed by V7.4.1) and asks Auth0 to drop the SSO session as defence-in-depth. `TokenFreshnessFilter` rejects any access token whose `iat` is before the cut-off and returns `401` with `WWW-Authenticate: Bearer error="invalid_token"`.
- Evidence:
    - [`UserService.java`](../../../../App/src/main/java/com/example/desofs/services/UserService.java)
    - [`TokenInvalidationService.java`](../../../../App/src/main/java/com/example/desofs/services/TokenInvalidationService.java)
    - [`TokenFreshnessFilter.java`](../../../../App/src/main/java/com/example/desofs/security/TokenFreshnessFilter.java)
    - [`Auth0ManagementClient.invalidateSessions`](../../../../App/src/main/java/com/example/desofs/security/Auth0ManagementClient.java)
    - [`V8__create_user_token_invalidations.sql`](../../../../App/src/main/resources/db/migration/V8__create_user_token_invalidations.sql)
    - [`TokenFreshnessFilterTest.java`](../../../../App/src/test/java/com/example/desofs/security/TokenFreshnessFilterTest.java)
    - [`TokenInvalidationServiceTest.java`](../../../../App/src/test/java/com/example/desofs/services/TokenInvalidationServiceTest.java)

---

## V8 - Authorization

### V8.3.2 - Changes to values on which authorization decisions are made are applied immediately; for self-contained tokens, mitigating controls must be in place

- Sprint 1 status: `Partial` (role changes only took effect on the next token issuance, i.e. up to one access-token lifetime later).
- Sprint 2 status: `Compliant`.
- Implementation: the same per-user cut-off mechanism described in V7.4.1 makes role changes take effect on the **next request**, not on the next token issuance. Because the JWT is self-contained, V8.3.2 explicitly accepts a mitigating control instead of in-token revocation: `TokenFreshnessFilter` plus the `user_token_invalidations` denylist play that role, and `UserService.assignRole` / `removeRole` call `invalidateUserSessions(...)` immediately after persisting the role mutation.
- Evidence: same as V7.4.1.

---

## V13 - Configuration

### V13.3.1 - Secrets management solution; secrets must not be included in application source code or build artifacts

- Sprint 1 status: `Partial` (no production deployment yet; secret handling existed only for CI).
- Sprint 2 status: `Compliant` for the deployment chain. (Sprint 1 reservations about HSM-backed storage at L3 still apply.)
- Implementation: GitHub Secrets -> SSH `envs:` -> [`docker-compose.prod.yml`](../../../../docker-compose.prod.yml). The runtime image carries no credentials; secrets never appear in the source tree, in image layers, or in workflow logs (masked with `::add-mask::` where they transit through outputs).
- Evidence:
    - [`.github/workflows/release-please.yml`](../../../../.github/workflows/release-please.yml)
    - [Pipeline Automation Cap4](../PipelineAutomation/pipelineAutomation.md#4-secrets-and-variables--compliance-statement)
    - [Security Configuration & Installation Cap3](../SecurityConfigurationAndInstallation/securityConfigurationAndInstallation.md#3-secrets-management)

---

## V15 - Secure Coding and Architecture

### V15.2.3 - Production environment only includes functionality required for the application to function

- Sprint 1 status: `Compliant` for the JAR; deployment surface not yet covered.
- Sprint 2 status: `Compliant` for the container deployment.
- Implementation: multi-stage Docker build keeps Maven and the JDK in the build stage and ships a JRE-only runtime image; container runs as the dedicated non-root user `emovieshop` with `security_opt: no-new-privileges:true` and `cap_drop: ALL`; only port 8080 is exposed; Swagger UI / OpenAPI remain disabled in production.
- Evidence:
    - [`App/Dockerfile`](../../../../App/Dockerfile)
    - [`App/docker-compose.yml`](../../../../App/docker-compose.yml)
    - [`docker-compose.prod.yml`](../../../../docker-compose.prod.yml)
    - [Security Configuration & Installation Cap2](../SecurityConfigurationAndInstallation/securityConfigurationAndInstallation.md#2-container-hardening)

---

## V16 - Security Logging and Error Handling

### V16.5.2 - The application continues to operate securely when external resource access fails (e.g., circuit breakers or graceful degradation)

- Sprint 1 status: `Partial`.
- Sprint 2 status: `Compliant` for the Auth0 Management dependency added by UC8.
- Implementation: `Auth0ManagementClient.invalidateSessions` swallows transport / 4xx / 5xx errors at WARN level so an Auth0 outage cannot roll back the local denylist write or block the role change. The local denylist remains the authoritative defence; the Auth0 call is best-effort.
- Evidence:
    - [`Auth0ManagementClient.java`](../../../../App/src/main/java/com/example/desofs/security/Auth0ManagementClient.java)
    - [`Auth0ManagementClientTest.java`](../../../../App/src/test/java/com/example/desofs/security/Auth0ManagementClientTest.java) (`invalidateSessions_swallowsAuth0Errors`, `invalidateSessions_rejectsInvalidUserId`)

---

## Carry-over from Sprint 1

All other ASVS 5.0.0 requirements assessed in Sprint 1 keep their previously recorded status. The full table is preserved at [`../../Sprint1/ASVSTraceability/Traceability.md`](../../Sprint1/ASVSTraceability/Traceability.md). Sprint 2 did not regress any control; the changes above are net improvements in coverage.
