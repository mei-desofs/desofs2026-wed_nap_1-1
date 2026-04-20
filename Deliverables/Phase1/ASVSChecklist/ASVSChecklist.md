# ASVS Checklist

The [OWASP Application Security Verification Standard (ASVS)](https://owasp.org/www-project-application-security-verification-standard/) was applied for **guidance purposes**.

This assessment was used to validate that the system's documented conception follows recognized security best practices, even though no implementation or code development has been completed at this stage. The objective is to ensure that design and security planning align with ASVS principles, establishing a strong foundation for the implementation phase.

The ASVS checklist was completed using the official tracker file **[ASVS_5.0_Tracker.xlsx](ASVS_5.0_Tracker.xlsx)**, which contains detailed per-requirement assessments with status, observations, and document references for each evaluated control.

---

## Table of Contents

1. [ASVS Levels Overview](#asvs-levels-overview)
2. [Evaluated Scope](#evaluated-scope)
3. [Level 3 Coverage Justification](#level-3-coverage-justification)
4. [Per-Category Analysis](#per-category-analysis)
   - [V1 - Encoding and Sanitization](#v1---encoding-and-sanitization)
   - [V2 - Validation and Business Logic](#v2---validation-and-business-logic)
   - [V3 - Web Frontend Security](#v3---web-frontend-security)
   - [V4 - API and Web Service](#v4---api-and-web-service)
   - [V5 - File Handling](#v5---file-handling)
   - [V6 - Authentication](#v6---authentication)
   - [V7 - Session Management](#v7---session-management)
   - [V8 - Authorization](#v8---authorization)
   - [V9 - Self-contained Tokens](#v9---self-contained-tokens)
   - [V10 - OAuth and OIDC](#v10---oauth-and-oidc)
   - [V11 - Cryptography](#v11---cryptography)
   - [V12 - Secure Communication](#v12---secure-communication)
   - [V13 - Configuration](#v13---configuration)
   - [V14 - Data Protection](#v14---data-protection)
   - [V15 - Secure Coding and Architecture](#v15---secure-coding-and-architecture)
   - [V16 - Security Logging and Error Handling](#v16---security-logging-and-error-handling)
   - [V17 - WebRTC](#v17---webrtc)
5. [Conclusion](#conclusion)

---

## ASVS Levels Overview

The ASVS framework defines three security assurance levels, each with increasing scope and rigor:

**Level 1 - Opportunistic (Basic)**
Provides the most basic level of security and is typically sufficient for small applications with low-security risks. It is often assigned to applications that do not deal with sensitive data, which makes them less of a target for attacks.

**Level 2 - Standard**
Provides a higher level of security and is typically required for applications with medium security risks. This often includes apps that conduct transactions or handle sensitive data that can be leveraged for financial gains by malicious users. Level 2 is usually assigned to applications susceptible to injections, validation, and authentication-based attacks.

**Level 3 - Advanced**
Provides the highest level of security and is typically required for applications with high-security risks. Level 3 requires the highest level of protection and contains highly sensitive information. For Level 3, security is often integrated at the beginning of the application pipeline, right through to production deployment, with automated monitoring as a secondary precaution. Each level inherits requirements from its predecessor.

## Evaluated Scope

All 17 ASVS v5.0.0 chapters were evaluated against the Phase 1 backend-only architecture. Compliance is assessed across all three ASVS levels, with each requirement receiving a documented status: **Compliant** (already addressed in design/documentation) or **Not Applicable** (not relevant to the current architecture).

| Chapter | Total Reqs | L1 Total | L1 Compliant | L1 % | L2 Total | L2 Compliant | L2 % | L3 Total | L3 Compliant | L3 % | Overall Compliant | Overall % |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| **V1** | 30 | 8 | 6 | 75% | 19 | 8 | 42% | 3 | 2 | 67% | 16 | 53% |
| **V2** | 13 | 4 | 4 | 100% | 7 | 7 | 100% | 2 | 1 | 50% | 12 | 92% |
| **V3** | 31 | 8 | 5 | 63% | 11 | 6 | 55% | 12 | 1 | 8% | 12 | 39% |
| **V4** | 16 | 2 | 1 | 50% | 8 | 3 | 38% | 6 | 5 | 83% | 9 | 56% |
| **V5** | 13 | 4 | 1 | 25% | 5 | 0 | 0% | 4 | 0 | 0% | 1 | 8% |
| **V6** | 47 | 13 | 11 | 85% | 22 | 8 | 36% | 12 | 3 | 25% | 22 | 47% |
| **V7** | 19 | 6 | 4 | 67% | 12 | 7 | 58% | 1 | 1 | 100% | 12 | 63% |
| **V8** | 13 | 4 | 4 | 100% | 3 | 2 | 67% | 6 | 1 | 17% | 7 | 54% |
| **V9** | 7 | 4 | 4 | 100% | 3 | 3 | 100% | 0 | N/A | N/A | 7 | 100% |
| **V10** | 36 | 5 | 5 | 100% | 24 | 15 | 63% | 7 | 0 | 0% | 20 | 56% |
| **V11** | 24 | 3 | 1 | 33% | 11 | 9 | 82% | 10 | 3 | 30% | 13 | 54% |
| **V12** | 12 | 3 | 3 | 100% | 6 | 4 | 67% | 3 | 0 | 0% | 7 | 58% |
| **V13** | 21 | 1 | 1 | 100% | 12 | 12 | 100% | 8 | 8 | 100% | 21 | 100% |
| **V14** | 13 | 2 | 1 | 50% | 7 | 5 | 71% | 4 | 0 | 0% | 6 | 46% |
| **V15** | 21 | 3 | 3 | 100% | 10 | 9 | 90% | 8 | 4 | 50% | 16 | 76% |
| **V16** | 17 | 0 | N/A | N/A | 16 | 16 | 100% | 1 | 1 | 100% | 17 | 100% |
| **V17** | 12 | 0 | N/A | N/A | 7 | 0 | 0% | 5 | 0 | 0% | 0 | 0% |
| | **TOTAL** | **345** | **70** | **54** | **76%** | **183** | **114** | **63%** | **92** | **30** | **39%** | **198** | **59%** |

**Compliance Distribution Visualization:**

[ASVSCoverageByLevel.png](ASVSCoverageByLevel.png)

**Overall Compliance Summary:**
Out of 345 ASVS requirements across all 17 chapters, **198 (59%)** are **Compliant**, distributed across three assurance levels:

- **Level 1 (Opportunistic):** 54 out of 70 applicable requirements compliant (**76%**)
- **Level 2 (Standard):** 114 out of 183 applicable requirements compliant (**63%**)
- **Level 3 (Advanced):** 30 out of 92 applicable requirements compliant (**39%**)

## Level 3 Coverage Justification

The notably lower compliance rate for **Level 3 (39% vs 76% for L1 and 63% for L2)** reflects an appropriately risk-calibrated approach to security assurance, aligned with the application's purpose and data classification:

**Application Context & Data Sensitivity:**

eMovieShop is a **digital movie retail platform** (videoclub) designed for customers to browse, purchase, and request refunds on physical movie copies. The system processes:
- **Customer purchase data:** Order history, receipt names, and transaction records (moderate sensitivity)
- **Movie catalog information:** Titles, genres, prices (capped at €500), and inventory quantities (low sensitivity)
- **Refund requests:** Reason and decision history (moderate sensitivity)
- **User roles and access control:** Role assignments (low sensitivity)

Notably, the application does **not handle**:
- Payment card information (payment processing is explicitly out-of-scope)
- Medical, financial, or highly sensitive personal information
- Government IDs, authentication credentials storage (delegated to Auth0)
- Real-time financial transactions
- Customer communications containing sensitive PII beyond what's necessary for orders

**Threat Model & Risk Assessment:**

The primary security threats are **application-level fraud scenarios**:
- Unauthorized purchases by other users (mitigated by RBAC, IDOR prevention at L2)
- Price tampering (mitigated by server-side validation at L2)
- Refund policy abuse (mitigated by business rule enforcement at L2)
- Unauthorized role escalation (mitigated by centralized RoleGuard at L2)

These threats are addressed comprehensively by **Level 2 controls** (63% coverage). Level 3 controls—such as advanced threat detection, multi-tenant isolation, fine-grained dynamic ABAC, and production-grade incident response with tamper-proof audit logging—are **over-provisioned for the actual business risk** of a modest-value retail transaction system.

**ASVS Level Appropriateness:**

- **Level 1 (76% coverage):** Baseline opportunistic security; addresses minimum protection needs.
- **Level 2 (63% coverage):** **Appropriate and sufficient target** for this application—ensures transaction integrity, input validation, strong authentication delegation, authorization enforcement, and adequate audit logging. Protects against common fraud scenarios and injection attacks.
- **Level 3 (39% coverage):** Advanced protection for high-value, highly-sensitive systems (e.g., healthcare, financial services, government). Unnecessary complexity for an academic movie retail system with moderate transaction values and non-sensitive data.

**Conclusion:**

Level 2 compliance is the **risk-appropriate target** for eMovieShop. The application's modest data sensitivity, bounded threat model (application-level fraud, not nation-state adversaries), and academic proof-of-concept scope justify this positioning. Level 3 requirements will remain relevant only if future phases introduce payment processing, PII aggregation, or multi-tenant isolation—none of which are currently planned.

## Per-Category Analysis

### V1 - Encoding and Sanitization (16/30 applicable)

The backend applies output encoding and input sanitization at the API boundary. Applicable requirements cover context-aware output encoding for JSON responses, prevention of injection attacks (SQLi, XSS in stored data), and safe handling of user-supplied strings in database queries via parameterized statements and JPA/Hibernate. The 14 Not Applicable requirements relate to frontend rendering contexts (HTML template injection, DOM-based encoding, CSS sanitization, JavaScript context encoding) that do not exist in a backend-only REST API.

### V2 - Validation and Business Logic (12/13 applicable)

Strong applicability with only 1 requirement marked N/A. The applicable requirements cover server-side input validation for all API requests, enforcement of business rules (movie price ≤ €500, max 10 items per order, 14-day refund window), type and range checks on all request parameters, and protection against mass assignment attacks. The design documentation explicitly defines validation rules for each aggregate's value objects (`MoviePrice`, `StockQuantity`, `MovieTitle`).

### V3 - Web Frontend Security (13/31 applicable, 18 N/A)

The highest N/A ratio, expected for a backend-only architecture. The 13 applicable requirements relate to HTTP security headers the backend API sets on responses: `Content-Security-Policy`, `X-Content-Type-Options`, CORS configuration, `Referrer-Policy`, and `Permissions-Policy`. The 18 N/A requirements concern client-side DOM manipulation, JavaScript execution contexts, iframe sandboxing, subresource integrity, and other browser-specific protections not relevant when there is no frontend.

> **Note:** If a frontend client were introduced, the N/A requirements in this chapter would become applicable and would need to be reassessed.

### V4 - API and Web Service (9/16 applicable)

The applicable requirements cover REST API design best practices: content-type validation, HTTP method enforcement, rate limiting for abuse prevention, request size limits, and proper use of HTTP status codes. The 7 N/A requirements relate to GraphQL-specific controls, SOAP/XML web services, and API gateway features not present in the current single-backend architecture.

### V5 - File Handling (1/13 applicable, 12 N/A)

Almost entirely not applicable since the system does not handle file uploads or downloads. The single applicable requirement relates to ensuring that file paths are not constructed from user-supplied input (path traversal prevention), which is relevant as a general secure coding practice even without file handling features.

### V6 - Authentication (11/47 applicable, 36 N/A)

Authentication is **delegated entirely to Auth0** as the external Identity Provider. The 11 applicable requirements cover the backend's responsibilities in this model: enforcing the password policy via Auth0 configuration (minimum 12 characters, bcrypt with work factor 12, no composition rules), account lockout after 10 failed attempts (Auth0 Brute Force Protection), secure handling of initial/temporary passwords, and ensuring credential transport over HTTPS only. The 36 N/A requirements relate to authentication mechanisms the backend does not implement directly (e.g., password hashing logic, credential recovery flows, lookup secret verifiers), these are managed by Auth0.

> **Auth0 handles internally:** password hashing (bcrypt, work factor 12, 128-bit salt), account lockout (Brute Force Protection after 10 attempts), breached password detection, credential transport (HTTPS-only hosted login page), and password policy enforcement (configured in Auth0 Dashboard).

### V7 - Session Management (12/19 applicable)

Session management is handled via JWT tokens issued by Auth0. The applicable requirements address role-based session expiration (Admin: 1h, Support: 2h, Customer: 6h), token verification on every request, session invalidation on logout (token revocation), federated session consistency with Auth0, and re-authentication for sensitive operations (verified via the `iat` claim). The 7 N/A requirements relate to cookie-based session management, UI-specific logout visibility, and user session management interfaces, not applicable in a backend-only API where tokens are sent via the `Authorization` header.

> **Auth0 handles internally:** SSO session management, refresh token issuance and rotation, and token revocation via `/oauth/revoke`. Session lifetime and inactivity timeout settings are configured to match the role-based expiration values.

> **Note:** If a frontend client were introduced, session cookies with `HttpOnly`, `Secure`, `SameSite=Lax`, and the `__Host-` prefix would be enforced, and several N/A requirements in this chapter would become applicable.

### V8 - Authorization (7/13 applicable)

All 7 applicable requirements are In Progress. They cover function-level access control (RBAC with Admin, Support, Customer roles), data-specific access (IDOR/BOLA defense, ownership verification for orders and refund requests), authorization enforcement at the service layer via `RoleGuard`, and immediate propagation of role changes. The 6 N/A requirements relate to contextual/adaptive authorization, multi-tenant isolation, and attribute-based access control (ABAC), features not present in the current single-tenant architecture.

### V9 - Self-contained Tokens (7/7 applicable)

Full applicability, all 7 requirements are directly relevant to the JWT-based architecture. They cover JWT signature validation against Auth0's JWKS endpoint, algorithm allowlisting (RS256), audience and issuer claim verification, token expiry enforcement, and rejection of unsigned or weakly signed tokens. This chapter is central to the security of the Auth0 integration.

### V10 - OAuth and OIDC (20/36 applicable)

A critical chapter for the Auth0 integration. The 14 Compliant and 6 In Progress requirements address the backend's role as an OAuth 2.0 Resource Server: validating access tokens from Auth0, enforcing scopes and audience claims, secure redirect URI handling, PKCE enforcement for authorization code flows, and proper token endpoint security. The 16 N/A requirements relate to features managed entirely by Auth0 (authorization server UI, consent screens, device authorization flows) or to OAuth grant types not used in the architecture.

> **Auth0 handles internally:** Authorization Code flow with PKCE, exact redirect URI matching, token issuance for configured API audiences, and refresh token rotation with automatic family revocation on reuse.

### V11 - Cryptography (13/24 applicable)

The backend does not implement custom cryptographic primitives, all cryptographic operations (JWT signing, password hashing, key management) are delegated to Auth0 and industry-validated libraries. The 13 applicable requirements cover key management documentation, cryptographic inventory, approved algorithm enforcement, secure failure handling, and data minimization. The 11 N/A requirements relate to custom block cipher modes, nonce management, constant-time operations, and post-quantum migration, none of which apply because the application code never directly touches low-level cryptographic primitives.

> **Auth0 handles internally:** JWT signing key management (RS256, automatic key rotation), public key publication via JWKS, and password hashing (bcrypt). The backend only verifies JWT signatures using the public keys from the JWKS endpoint.

### V12 - Secure Communication (7/12 applicable)

Applicable requirements cover TLS 1.2/1.3 enforcement, cipher suite configuration, mandatory HTTPS with no fallback to plaintext, publicly trusted certificates, and certificate validation for outbound connections (e.g., Auth0 JWKS endpoint). The 5 N/A requirements relate to mTLS, OCSP stapling, Encrypted Client Hello (ECH), and service mesh authentication, L3 controls or microservice-specific concerns not relevant to the single-backend architecture.

### V13 - Configuration (14/21 applicable)

Applicable requirements address secure default configuration, HTTP security headers on API responses, dependency management and vulnerability scanning (OWASP Dependency-Check, Snyk), environment variable-based secret management, and build pipeline security (GitHub Actions with Semgrep, Dependabot). The 7 N/A requirements relate to frontend build configurations, CDN configuration, and multi-environment deployment controls not applicable in Phase 1.

### V14 - Data Protection (6/13 applicable)

Applicable requirements cover sensitive data classification (user credentials, purchase history, refund data), protection requirements per classification level, HTTP body-only transport for sensitive data (no sensitive data in URLs or query strings), and third-party data minimization (only essential claims sent to Auth0). The 7 N/A requirements are predominantly client-side (browser storage cleanup, anti-caching headers for browser clients), not applicable in a backend-only architecture, plus L3 controls like data masking and automated retention/deletion scheduling.

### V15 - Secure Coding and Architecture (12/21 applicable)

Applicable requirements address the DDD-based layered architecture (Controller → Service → Repository), separation of concerns, secure dependency injection, input validation at system boundaries, and centralized security logic in the Auth Middleware and `RoleGuard`. The 9 N/A requirements relate to frontend architecture patterns, client-side state management, and UI component security, not applicable without a frontend.

### V16 - Security Logging and Error Handling (16/17 applicable)

Near-full applicability. Applicable requirements cover structured security event logging (authentication events, authorization failures, refund operations, role changes), audit trail integrity, safe error responses that do not leak stack traces or internal details, centralized log management, and log protection against tampering. The single N/A requirement relates to client-side error reporting, which does not apply to a backend-only architecture.

### V17 - WebRTC (0/12, fully N/A)

All 12 requirements are Not Applicable. The system does not include any WebRTC components or real-time communication features.

## Conclusion

The ASVS checklist was applied comprehensively across all 17 chapters of ASVS v5.0.0 to validate the secure conception of the eMovieShop system. All 345 requirements received a documented assessment, 176 are applicable to the current architecture and 169 are explicitly marked as Not Applicable with documented justification.

The high N/A rate (49%) is a direct consequence of the backend-only, API-only architecture: the absence of a frontend client makes V3 (Web Frontend Security), V5 (File Handling), and V17 (WebRTC) largely irrelevant, while the delegation of authentication to Auth0 reduces V6 (Authentication) to configuration-level controls.

Among the 176 applicable requirements, 48 are already Compliant in the current design documentation and Auth0 configuration, while 128 are In Progress with design decisions documented and ready for implementation. The strongest coverage is in V9 (Self-contained Tokens), V10 (OAuth and OIDC), and V2 (Validation and Business Logic), reflecting the architecture's reliance on JWT-based authentication via Auth0 and well-defined business rules. Detailed per-requirement assessments, observations, and document references are available in the companion tracker file **[ASVS_5.0_Tracker.xlsx](ASVS_5.0_Tracker.xlsx)**.
