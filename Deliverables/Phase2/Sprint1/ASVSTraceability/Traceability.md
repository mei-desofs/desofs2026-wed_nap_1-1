# ASVS Traceability: Implementations → Checklist

This documentnt ensusres completeness of the ASVS assessment and provsides
traceability between implemented security controls, documented security
requirements,, documentests that validate themnts, and the tests that 
alidate them. The sections below map concrete code-level implementation
 to the ASVS areas used in the project and provide rationale and operation
l notes so that reviewers and auditors can

## Traceability Model

- Security control: what protection or safeguard is implemented.
- Implementation: where the control lives in the codebase.
- ASVS mapping: which checklist area(s) the control supports.
- Evidence / tests: what validates the behaviour in practice.
- Status: `Implemented`, `Partial`, or `N/A`.
validate coverage.

## Traceability Model

- Security control: what protection or safeguard is implemented.
- Implementation: where the control lives in the codebase.
- ASVS mapping: which checklist area(s) the control supports.
- Evidence / tests: what validates the behaviour in practice.
- Status: `Implemented`, `Partial`, or `N/A`.

---

## RateLimitFilter (request throttling)

Implementation: [App/src/main/java/com/example/desofs/security/RateLimitFilter.java](App/src/main/java/com/example/desofs/security/RateLimitFilter.java)

- Controls implemented:
	- Per-IP token-bucket rate limiting
	- Per-authenticated-user token-bucket rate limiting
	- Configurable thresholds and flags (application properties)
	- `Retry-After` header on throttled responses
	- Minimal/non-sensitive 429 responses and logging guidance
	- Configurable `trustForwardedHeaders` for proxy deployments

- ASVS areas mapped:
	- V4 - API and Web Service (rate limiting, 429 responses)
	- V13 - Configuration (externalized thresholds and flags)
	- V16 - Security Logging and Error Handling (controlled responses and logging guidance)

- Rationale and notes:
	- Token-bucket smoothing reduces burst impact while enforcing per-entity
		quotas; separating IP and authenticated-user buckets addresses both
		anonymous floods and credentialed abuse.
	- `X-Forwarded-For` handling must only be enabled when the app sits behind
		a trusted proxy — improper use allows request spoofing.
	- Add monitoring/alerts for frequent 429 events to detect attacks or
		misconfiguration.

---

## SecurityHeadersFilter (HTTP security headers)

Implementation: [App/src/main/java/com/example/desofs/security/SecurityHeadersFilter.java](App/src/main/java/com/example/desofs/security/SecurityHeadersFilter.java)

- Controls implemented:
	- Content-Security-Policy (CSP)
	- Cross-Origin Resource Sharing (CORS) restrictions (allowed origins)
	- X-Frame-Options: DENY (clickjacking protection)
	- Strict-Transport-Security (HSTS)
	- X-Content-Type-Options: nosniff
	- X-XSS-Protection: 1; mode=block (legacy)
	- Referrer-Policy: no-referrer

- ASVS areas mapped:
	- V4 - API and Web Service (CSP, clickjacking, content-type protection)
	- V12 - Secure Communication (HSTS)
	- V13 - Configuration (CORS and CSP are configurable)
	- V16 - Security Logging and Error Handling (referrer policy reduces leakage)

- Rationale and notes:
	- CSP must be tuned per-application; overly permissive CSP or CORS
		settings weaken protection and should be validated during deployment.
	- HSTS must only be enabled once HTTPS is fully configured to avoid
		locking clients into broken configurations.
	- Consider reporting CSP violations to a monitored endpoint if desired
		(requires additional implementation and privacy considerations).

---

## AuditLogService (security auditing)

Implementation: [App/src/main/java/com/example/desofs/services/AuditLogService.java](App/src/main/java/com/example/desofs/services/AuditLogService.java)

- Controls implemented:
	- Audit log persistence for role assignment and removal operations.
	- Centralized mapping from `AuditLog` domain objects to `AuditLogDTO`.
	- Explicit role-operation entries (`ASSIGN`, `REMOVE`) for accountability.

- ASVS areas mapped:
	- V8 - Data Protection and Privacy (auditable security events and traceable changes)
	- V10 - Malicious Code / Monitoring support where audit trails help investigate abuse
	- V16 - Security Logging and Error Handling (security-relevant events are recorded)

- Rationale and notes:
	- Audit entries help reconstruct privileged actions and support incident
		investigation.
	- The audit log is domain-scoped to role changes, which are high-value events
		for accountability.

---

## ReceiptFileService (file handling)

Implementation: [App/src/main/java/com/example/desofs/services/ReceiptFileService.java](App/src/main/java/com/example/desofs/services/ReceiptFileService.java)

- Controls implemented:
	- Allow-list sanitization of receipt names.
	- Sandboxed directory creation for generated files.
	- Path traversal protection via normalized path checks.
	- `CREATE_NEW` file creation to prevent overwrite attacks.
	- Fixed receipt formatting with bounded filename length.

- ASVS areas mapped:
	- V8 - Data Protection and Privacy (safe handling of stored/generated files)
	- V10 - Malicious Code (path traversal and unsafe file write prevention)
	- V13 - Configuration (sandbox directory and name-length constraints are configurable)
	- V16 - Security Logging and Error Handling (path traversal attempts are logged)

- Rationale and notes:
	- This is the clearest file-handling surface in the current backend and is a
		good example of file-write hardening.
	- The allow-list strategy and sandbox path checks reduce the attack surface
		for arbitrary file creation.

---

## SecurityConfig (authentication, authorization, transport)

Implementation: [App/src/main/java/com/example/desofs/config/SecurityConfig.java](App/src/main/java/com/example/desofs/config/SecurityConfig.java)

- Controls implemented:
	- JWT resource-server authentication with issuer and audience validation.
	- Stateless session management.
	- Method security enabled for role-based authorization.
	- Security headers configured at the framework level, including HSTS.
	- Rate limiting filter inserted into the security chain.

- ASVS areas mapped:
	- V2 - Authentication (JWT validation and principal handling)
	- V4 - Access Control / API and Web Service (authenticated endpoints and security chain hardening)
	- V12 - Secure Communication (HSTS and transport hardening)
	- V13 - Configuration (issuer, audience, and filter wiring are property-driven)

- Rationale and notes:
	- JWT validation against issuer and audience prevents token confusion across
		environments.
	- Stateless sessions reduce session fixation and server-side state risk.
	- Method security complements endpoint rules and should be reflected in tests
		for privileged operations.

---

## Cross-cutting concerns

### Logging and monitoring

- Relevant implementations:
	- [App/src/main/java/com/example/desofs/security/RateLimitFilter.java](App/src/main/java/com/example/desofs/security/RateLimitFilter.java)
	- [App/src/main/java/com/example/desofs/security/SecurityHeadersFilter.java](App/src/main/java/com/example/desofs/security/SecurityHeadersFilter.java)
	- [App/src/main/java/com/example/desofs/services/AuditLogService.java](App/src/main/java/com/example/desofs/services/AuditLogService.java)
	- [App/src/main/java/com/example/desofs/services/ReceiptFileService.java](App/src/main/java/com/example/desofs/services/ReceiptFileService.java)

- ASVS areas mapped:
	- V16 - Security Logging and Error Handling

### Authentication and authorization

- Relevant implementations:
	- [App/src/main/java/com/example/desofs/config/SecurityConfig.java](App/src/main/java/com/example/desofs/config/SecurityConfig.java)
	- Controllers guarded with role checks and method security

- ASVS areas mapped:
	- V2 - Authentication
	- V4 - Access Control

- What to document in tests:
	- 401/403 behaviour for unauthenticated and unauthorized requests.
	- Role-based access to admin and staff flows.

### File handling

- Relevant implementation:
	- [App/src/main/java/com/example/desofs/services/ReceiptFileService.java](App/src/main/java/com/example/desofs/services/ReceiptFileService.java)

- ASVS areas mapped:
	- V8 - Data Protection and Privacy
	- V10 - Malicious Code
	- V13 - Configuration

### Transport and browser hardening

- Relevant implementations:
	- [App/src/main/java/com/example/desofs/config/SecurityConfig.java](App/src/main/java/com/example/desofs/config/SecurityConfig.java)
	- [App/src/main/java/com/example/desofs/security/SecurityHeadersFilter.java](App/src/main/java/com/example/desofs/security/SecurityHeadersFilter.java)

- ASVS areas mapped:
	- V4 - API and Web Service
	- V12 - Secure Communication
	- V13 - Configuration

- What to document in tests:
	- HSTS and header presence.
	- CORS policy for trusted origins only.
	- CSP emission for representative responses.

### Error handling and response hygiene

- Relevant implementations:
	- [App/src/main/java/com/example/desofs/security/RateLimitFilter.java](App/src/main/java/com/example/desofs/security/RateLimitFilter.java)
	- [App/src/main/java/com/example/desofs/security/SecurityHeadersFilter.java](App/src/main/java/com/example/desofs/security/SecurityHeadersFilter.java)
	- Controllers and services that return controlled 4xx/5xx responses

- ASVS areas mapped:
	- V16 - Security Logging and Error Handling

---




