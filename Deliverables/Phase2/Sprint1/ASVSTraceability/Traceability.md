# ASVS Traceability: Implementations → Checklist

This document ensures completeness of the ASVS assessment and provides
traceability between implemented security controls, documented security
requirements, and the tests that validate them.

## Traceability Model

- Security control: what protection or safeguard is implemented.
- Implementation: where the control lives in the codebase.
- ASVS mapping: which checklist area(s) the control supports.
- Evidence / tests: what validates the behaviour in practice.
- Status: `Compliant`, `Partial`, `Not Applicable`.

---

## Index

Jump to section:

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
- [V11 - Cryptographic Inventory and Documentation](#v11---cryptographic-inventory-and-documentation)
- [V12 - Secure Communication](#v12---secure-communication)
- [V13 - Configuration](#v13---configuration)
- [V14 - Data Protection](#v14---data-protection)
- [V15 - Secure Coding and Architecture](#v15---secure-coding-and-architecture)
- [V16 - Security Logging and Error Handling](#v16---security-logging-and-error-handling)


## V1 - Encoding and Sanitization

This section maps the ASVS V1 - Encoding and Sanitization - checklist section to implemented controls and evidence in the codebase. Each item below lists the ASVS requirement identifier, a short statement of compliance, the implementation locations, and available evidence/tests.


### V1.1 - Encoding and Sanitization Architecture

#### V1.1.1 - Input canonicalisation performed once
- Status: `Compliant`
- Implementation: canonicalisation and normalization take place at the API boundary via DTO validation and sanitizers. Relevant code: `App/src/main/java/com/example/desofs/shared/dtos/MovieDTO.java`, `App/src/main/java/com/example/desofs/controllers/MovieController.java`, and `App/src/main/java/com/example/desofs/services/ReceiptFileService.java` (sanitizers used for filenames).
- Evidence / Tests:
	- `App/src/test/java/com/example/desofs/controller/InputValidationSecurityTest.java` - tests for path traversal, null bytes and XSS neutralization when creating orders (receiptName sanitization).
	- `App/src/test/java/com/example/desofs/services/ReceiptFileServiceTest.java` - unit tests for `sanitizeReceiptName()` (path traversal, null bytes, allowed characters).

#### V1.1.2 - Output encoding and escaping is final step
- Status: `Compliant`
- Implementation: responses are JSON serialized via Spring Boot `Jackson` config; security headers enforced by `SecurityHeadersFilter` (`App/src/main/java/com/example/desofs/security/SecurityHeadersFilter.java`).
- Evidence / Tests:
	- `App/src/test/java/com/example/desofs/security/SecurityHeadersFilterTest.java` - asserts CSP, HSTS and other headers are set.
	- Integration tests under `App/src/test/java/com/example/desofs/controller/` validate JSON responses and content-type headers (e.g., `MovieControllerIntegrationTests`).

### V1.2 - Injection Prevention

#### V1.2.1 - Contextual output encoding for responses
- Status: `Compliant`
- Implementation: API returns JSON; HTML/XML are not produced by server-side templates. `SecurityHeadersFilter` provides CSP and content-type protections.
- Evidence / Tests:
	- `App/src/test/java/com/example/desofs/security/SecurityHeadersFilterTest.java` - header assertions.
	- `App/src/test/java/com/example/desofs/controller/MovieControllerIntegrationTests.java` - response assertions for JSON content.

#### V1.2.2 - Safe URL handling and building
- Status: `Compliant`
- Implementation: controllers build resource URIs (see `MovieController.create()` in `App/src/main/java/com/example/desofs/controllers/MovieController.java`). Where possible the code uses typed URIs; review notes flag `URI.create("/api/movies/" + id)` as the Location construction point.
- Evidence / Tests:
	- `App/src/test/java/com/example/desofs/controller/MovieControllerIntegrationTests.java` - asserts 201 Created and presence/format of `Location` header after POST.

#### V1.2.3 - Encoding when building JavaScript/JSON content
- Status: `Compliant`
- Implementation: API only returns JSON using DTOs; no server-side JS templates or inline JS generation.
- Evidence / Tests:
	- Controller integration tests validating JSON response bodies (see `MovieControllerIntegrationTests`).

#### V1.2.4 - Parameterized queries / ORM usage
- Status: `Compliant`
- Implementation: persistence layer uses Spring Data JPA repositories (`App/src/main/java/com/example/desofs/repositories/MovieRepository.java`, `OrderRepository.java`, etc.). No raw string SQL concatenation discovered in codebase.
- Evidence / Tests:
	- Repository interfaces under `App/src/main/java/com/example/desofs/repositories/`.
	- Integration tests exercising repositories via Spring context (e.g., `InputValidationSecurityTest` initialises test data via `MovieRepository`).

#### V1.2.5 - OS command injection protections
- Status: `Not Applicable`
- Implementation: the application does not perform arbitrary OS command execution in request flows. File writes are restricted and sandboxed by `ReceiptFileService`.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/services/ReceiptFileService.java` and `App/src/test/java/com/example/desofs/services/ReceiptFileServiceTest.java` (tests cover path traversal and create semantics).

#### V1.2.9 - Regex special-character handling
- Status: `Compliant`
- Implementation: user-supplied values used in pattern contexts are validated/escaped by allow-list sanitizers (see `ReceiptFileService` and related validators).
- Evidence / Tests:
	- `App/src/test/java/com/example/desofs/services/ReceiptFileServiceTest.java` - tests that inputs containing metacharacters are normalized.

### V1.3 - Sanitization

#### V1.3.2 - Avoidance of dynamic code execution (eval/SpEL)
- Status: `Compliant`
- Implementation: no usage of dynamic evaluation APIs (SpEL eval) or `javax.script` for user-controlled input identified in the codebase.
- Evidence / Tests:
	- Codebase search shows no `ExpressionParser` or `SpEL` usage; relevant checks in static analysis reports (CodeQL/FindSecBugs) executed in CI.

#### V1.3.3 - Validation before dangerous contexts
- Status: `Compliant`
- Implementation: DTO bean validation annotations (e.g., `MovieDTO`) and controller-level `@Valid` ensure inputs are validated before use.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/shared/dtos/MovieDTO.java` and `App/src/test/java/com/example/desofs/controller/MovieControllerIntegrationTests.java`.
	- `App/src/test/java/com/example/desofs/controller/InputValidationSecurityTest.java` covers many sanitization cases.

#### V1.3.6 - SSRF protections (where applicable)
- Status: `Not Applicable`
- Implementation: the application does not expose generic URL-fetching endpoints that accept arbitrary hosts. Any outbound calls are to trusted backends or validated sources (none found that accept open URLs).
- Evidence / Tests:
	- Codebase review: no endpoints accepting arbitrary external URLs; CI SAST scans would flag unsafe HTTP client usage.

#### V1.3.10 - Format-string sanitization
- Status: `Compliant`
- Implementation: server-side formatting avoids user-controlled format strings; user content is treated as data in DTOs and serialized safely.
- Evidence / Tests:
	- Review of controller/service formatting sites and integration tests asserting outputs.

#### V1.3.12 - ReDoS / runaway regex mitigations
- Status: `Compliant`
- Implementation: sanitizers limit allowed character sets and length for inputs used in regex contexts; critical regexes avoid catastrophic backtracking patterns.
- Evidence / Tests:
	- `ReceiptFileServiceTest.java` validates behavior for problematic inputs; code review of regex usage in the repo.

### V1.4 / V1.5 - Memory, Integer overflows and Deserialization

#### V1.4.2 - Integer overflow sign/range checks
- Status: `Compliant`
- Implementation: DTOs and service logic use numeric validation (`@Min`, `@NotNull`) and explicit range checks where quantities are critical (orders). Integration tests cover zero/negative/very large quantities.
- Evidence / Tests:
	- `App/src/test/java/com/example/desofs/controller/InputValidationSecurityTest.java` - quantity manipulation tests (zero, negative, extremely large values).

#### V1.5.2 / V1.5.3 - Safe deserialization and parser consistency
- Status: `Compliant`
- Implementation: JSON deserialization uses Jackson with typed DTOs; the application does not accept arbitrary polymorphic types from untrusted input. Parsers are consistently configured in Spring Boot defaults.
- Evidence / Tests:
	- Jackson configuration review and integration tests that post JSON payloads (`MovieControllerIntegrationTests`, `InputValidationSecurityTest`).

## V2 - Validation and Business Logic

### V2.1 - Validation and Business Logic Documentation

#### V2.1.1 - Verify that the application's documentation defines input validation rules for how to check the validity of data items against an expected structure. This could be common data formats such as credit card numbers, email addresses, telephone numbers, or it could be an internal data format.

- Status: `Partial`
- Implementation: high-level validation guidance exists in project documentation (e.g., `Deliverables/Phase2/Sprint1/Development/development.md` and README sections) describing DTO-based validation and examples. The docs describe what is validated (examples in `MovieDTO`) but do not enumerate every field format (email/phone/CC) exhaustively.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/shared/dtos/MovieDTO.java` - examples of bean validation annotations used as the canonical rules.
	- Documentation: `Deliverables/Phase2/Sprint1/Development/development.md` - validation and API design notes.

#### V2.1.2 - Verify that the application's documentation defines how to validate the logical and contextual consistency of combined data items, such as checking that suburb and ZIP code match.

- Status: `Partial`
- Implementation: business-rule expectations are described at a high level in design docs, but contextual cross-field validation is primarily implemented in service layer code (where applicable) rather than exhaustively documented.
- Evidence / Tests:
	- `App/src/test/java/com/example/desofs/controller/InputValidationSecurityTest.java` - tests exercising combined-field validation scenarios (order quantities vs. available inventory).

#### V2.1.3 - Verify that expectations for business logic limits and validations are documented, including both per-user and globally across the application.

- Status: `Partial`
- Implementation: limits and guards (e.g., maximum quantities, rate limits) are enforced in code and are described in deployment/architecture docs, but a single consolidated policy document is not present.
- Evidence / Tests:
	- `App/src/test/java/com/example/desofs/controller/InputValidationSecurityTest.java` - quantity limit tests.
	- `App/src/main/java/com/example/desofs/security/RateLimitFilter.java` - rate-limit implementation referenced in docs.

### V2.2 - Input Validation

#### V2.2.1 - Verify that input is validated to enforce business or functional expectations for that input. This should either use positive validation against an allow list of values, patterns, and ranges, or be based on comparing the input to an expected structure and logical limits according to predefined rules. For L1, this can focus on input which is used to make specific business or security decisions. For L2 and up, this should apply to all input.

- Status: `Compliant`
- Implementation: input validation is enforced at the API boundary via bean validation on DTOs (`@NotBlank`, `@NotNull`, `@Min`, `@DecimalMin`) and additional allow-list sanitizers in services (e.g., filename sanitization in `ReceiptFileService`).
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/shared/dtos/MovieDTO.java` - DTO validation annotations.
	- `App/src/test/java/com/example/desofs/controller/MovieControllerIntegrationTests.java` - integration tests asserting validation behaviour.
	- `App/src/test/java/com/example/desofs/services/ReceiptFileServiceTest.java` - allow-list sanitization unit tests.

#### V2.2.2 - Verify that the application is designed to enforce input validation at a trusted service layer. While client-side validation improves usability and should be encouraged, it must not be relied upon as a security control.

- Status: `Partial`
- Implementation: controllers validate inputs via `@Valid` and DTOs; service layer methods perform additional checks for critical flows (orders, receipts). Some checks are present in services, but not every DTO has an explicit service-level validator wrapper.
- Evidence / Tests:
	- `App/src/test/java/com/example/desofs/controller/InputValidationSecurityTest.java` - demonstrates controller + service interactions for validation.
	- `App/src/main/java/com/example/desofs/services/ReceiptFileService.java` - example of service-side sanitization and checks.

#### V2.2.3 - Verify that the application ensures that combinations of related data items are reasonable according to the pre-defined rules.

- Status: `Partial`
- Implementation: combination checks (e.g., order quantity vs. inventory) are implemented in business services and covered by integration tests, but coverage is not exhaustive for all possible combinations.
- Evidence / Tests:
	- `App/src/test/java/com/example/desofs/controller/InputValidationSecurityTest.java` - tests that exercise combined-field rules.
	- `App/src/test/java/com/example/desofs/controller/OrderControllerIntegrationTests.java` - business-flow tests that validate order rules.

### V2.3 - Business Logic Security

#### V2.3.1 - Verify that the application will only process business logic flows for the same user in the expected sequential step order and without skipping steps.

- Status: `Partial`
- Implementation: endpoint and service design enforce typical request flows (session/user-scoped operations). Explicit state-machine enforcement for multi-step flows is implemented only where needed by the use case; not all flows use a strict step-enforcement mechanism.
- Evidence / Tests:
	- `App/src/test/java/com/example/desofs/controller/OrderControllerIntegrationTests.java` - tests covering typical order flow sequences.

#### V2.3.2 - Verify that business logic limits are implemented per the application's documentation to avoid business logic flaws being exploited.

- Status: `Compliant`
- Implementation: business limits such as quantity caps and validation are enforced in DTOs/services and tested in integration tests.
- Evidence / Tests:
	- `App/src/test/java/com/example/desofs/controller/InputValidationSecurityTest.java` - tests for quantity and limit validation.
	- `App/src/test/java/com/example/desofs/controller/OrderControllerIntegrationTests.java` - end-to-end order limit checks.

#### V2.3.3 - Verify that transactions are being used at the business logic level such that either a business logic operation succeeds in its entirety or it is rolled back to the previous correct state.

- Status: `Partial`
- Implementation: persistence operations are performed via Spring Data JPA and transactional semantics are relied upon; some service methods are transactional and generic transaction rollback behaviour is covered by `GlobalExceptionHandler` tests. A focused audit of all service methods for `@Transactional` presence is recommended.
- Evidence / Tests:
	- `App/src/test/java/com/example/desofs/config/SecurityConfigIntegrationTest.txt` and `App/src/test/java/com/example/desofs/controller/OrderControllerIntegrationTests.java` - integration tests that exercise commit/rollback scenarios.
	- `App/src/test/java/com/example/desofs/GlobalExceptionHandlerTest.java` - handles transaction-related exceptions in tests.

#### V2.3.4 - Verify that business logic level locking mechanisms are used to ensure that limited quantity resources (such as theater seats or delivery slots) cannot be double-booked by manipulating the application's logic.

- Status: `Not Applicable`
- Implementation: the application does not implement complex allocation/booking resources requiring distributed locks; quantity checks are enforced but no explicit locking mechanism (pessimistic/optimistic lock) was found for seat-like resources.
- Evidence / Tests:
	- Codebase review: no `@Lock`, optimistic/pessimistic locking or explicit distributed lock APIs detected.

### V2.4 - Anti-automation

#### V2.4.1 - Verify that anti-automation controls are in place to protect against excessive calls to application functions that could lead to data exfiltration, garbage-data creation, quota exhaustion, rate-limit breaches, denial-of-service, or overuse of costly resources.

- Status: `Compliant`
- Implementation: `RateLimitFilter` enforces per-request rate limiting at the API gateway/filter level; CI and infra docs reference rate-limiting as a deployment control.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/security/RateLimitFilter.java` - implementation.
	- `App/src/test/java/com/example/desofs/security/RateLimitFilterTest.java` - unit tests asserting rate-limit behaviour.

#### V2.4.2 - Verify that business logic flows require realistic human timing, preventing excessively rapid transaction submissions.

- Status: `Partial`
- Implementation: rate-limiting provides a coarse control against rapid submissions; there is no explicit human-timing enforcement (e.g., progressive delays, captcha) for high-risk flows in the application code itself.
- Evidence / Tests:
	- `App/src/test/java/com/example/desofs/security/RateLimitFilterTest.java` - demonstrates protection against rapid repeated requests.
	- Deployment docs/CI reference rate limits as part of operational controls.

## V3 - Web Frontend Security

### V3.3 - Cookie Setup

#### V3.3.1 - Verify that cookies have the 'Secure' attribute set, and if the '\__Host-' prefix is not used for the cookie name, the '__Secure-' prefix must be used for the cookie name.

- Status: `Partial`
- Implementation: application-level cookie attributes are controlled by framework defaults and deployment (Tomcat/Spring Security). The codebase does not explicitly prefix cookies with `__Host-` or `__Secure-` for application cookies.
- Evidence / Tests:
	- `App/src/test/java/com/example/desofs/security/SecurityHeadersFilterTest.java` - integration/unit-level tests for headers; cookie-specific tests are not present.
	- Deployment note: Tomcat/Spring Boot session cookie configuration is expected to be set in `application.properties` or environment during deployment.

#### V3.3.2 - Verify that each cookie's 'SameSite' attribute value is set according to the purpose of the cookie, to limit exposure to user interface redress attacks and browser-based request forgery attacks, commonly known as cross-site request forgery (CSRF).

- Status: `Partial`
- Implementation: `SameSite` is not explicitly set by application code; behaviour depends on container/framework defaults and runtime configuration. CSRF protections are provided via Spring Security CSRF tokens for state-changing endpoints.
- Evidence / Tests:
	- `App/src/test/java/com/example/desofs/security/SecurityHeadersFilterTest.java` - headers test (does not assert SameSite).
	- Spring Security CSRF integration tests present in controller integration tests (e.g., `MovieControllerIntegrationTests`).

#### V3.3.3 - Verify that cookies have the '__Host-' prefix for the cookie name unless they are explicitly designed to be shared with other hosts.

- Status: `Not Applicable`
- Implementation: application does not create host-prefixed cookies; cookies created by the framework are standard session cookies managed by container.
- Evidence / Tests:
	- Codebase review: no explicit cookie naming with `__Host-`/`__Secure-` prefixes.

#### V3.3.4 - Verify that if the value of a cookie is not meant to be accessible to client-side scripts (such as a session token), the cookie must have the 'HttpOnly' attribute set and the same value (e. g. session token) must only be transferred to the client via the 'Set-Cookie' header field.

- Status: `Partial`
- Implementation: session cookies are typically set `HttpOnly` by container defaults; the application does not explicitly set `HttpOnly` on custom cookies in code.
- Evidence / Tests:
	- `App/src/test/java/com/example/desofs/security/SecurityHeadersFilterTest.java` - header tests; no explicit cookie attribute assertions found.
	- Deployment/container configuration expected to enforce `HttpOnly` for session cookies.

#### V3.3.5 - Verify that when the application writes a cookie, the cookie name and value length combined are not over 4096 bytes. Overly large cookies will not be stored by the browser and therefore not sent with requests, preventing the user from using application functionality which relies on that cookie.

- Status: `Partial`
- Implementation: the application does not set large cookies; session data is not stored in cookies. No explicit size checks in code.
- Evidence / Tests:
	- Codebase review: no functionality writing large cookies detected.
	- No unit tests asserting cookie size limits.

### V3.4 - Browser Security Mechanism Headers

#### V3.4.1 - Verify that a Strict-Transport-Security header field is included on all responses to enforce an HTTP Strict Transport Security (HSTS) policy. A maximum age of at least 1 year must be defined, and for L2 and up, the policy must apply to all subdomains as well.

- Status: `Compliant`
- Implementation: `SecurityHeadersFilter` sets HSTS header fields and is applied to responses in the web layer.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/security/SecurityHeadersFilter.java` - sets HSTS header.
	- `App/src/test/java/com/example/desofs/security/SecurityHeadersFilterTest.java` - asserts HSTS presence.

#### V3.4.2 - Verify that the Cross-Origin Resource Sharing (CORS) Access-Control-Allow-Origin header field is a fixed value by the application, or if the Origin HTTP request header field value is used, it is validated against an allowlist of trusted origins. When 'Access-Control-Allow-Origin: *' needs to be used, verify that the response does not include any sensitive information.

- Status: `Partial`
- Implementation: CORS configuration is handled via Spring MVC/CORS configuration where required; no global fixed-origin allowlist file found in the codebase. The app relies on framework config and environment-specific settings.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/config` - potential CORS configuration classes (review required for exact class).
	- Integration tests implicitly validate CORS behaviour when run in integration contexts, but no explicit allowlist tests found.

#### V3.4.4 - Verify that all HTTP responses contain an 'X-Content-Type-Options: nosniff' header field. This instructs browsers not to use content sniffing and MIME type guessing for the given response, and to require the response's Content-Type header field value to match the destination resource. For example, the response to a request for a style is only accepted if the response's Content-Type is 'text/css'. This also enables the use of the Cross-Origin Read Blocking (CORB) functionality by the browser.

- Status: `Compliant`
- Implementation: `SecurityHeadersFilter` sets `X-Content-Type-Options: nosniff` on responses.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/security/SecurityHeadersFilter.java` - header implementation.
	- `App/src/test/java/com/example/desofs/security/SecurityHeadersFilterTest.java` - asserts `nosniff` header.

#### V3.4.5 - Verify that the application sets a referrer policy to prevent leakage of technically sensitive data to third-party services via the 'Referer' HTTP request header field. This can be done using the Referrer-Policy HTTP response header field or via HTML element attributes. Sensitive data could include path and query data in the URL, and for internal non-public applications also the hostname.

- Status: `Partial`
- Implementation: `SecurityHeadersFilter` may set referrer-related headers; explicit `Referrer-Policy` header assertions are not present in tests. Recommend adding an explicit `Referrer-Policy` header if required by policy.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/security/SecurityHeadersFilter.java` - review for referrer header behaviour.
	- No explicit unit tests asserting `Referrer-Policy`.

### V3.5 - Browser Origin Separation

#### V3.5.2 - Verify that, if the application relies on the CORS preflight mechanism to prevent disallowed cross-origin use of sensitive functionality, it is not possible to call the functionality with a request which does not trigger a CORS-preflight request. This may require checking the values of the 'Origin' and 'Content-Type' request header fields or using an extra header field that is not a CORS-safelisted header-field.

- Status: `Partial`
- Implementation: CORS protections are applied via Spring configuration where endpoints expose cross-origin APIs. A full audit of all endpoints to ensure preflight-triggering headers are validated is recommended.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/config` - CORS configuration candidates.
	- Integration tests may implicitly exercise CORS flows; no explicit CORS-preflight unit tests found.

#### V3.5.3 - Verify that HTTP requests to sensitive functionality use appropriate HTTP methods such as POST, PUT, PATCH, or DELETE, and not methods defined by the HTTP specification as "safe" such as HEAD, OPTIONS, or GET. Alternatively, strict validation of the Sec-Fetch-* request header fields can be used to ensure that the request did not originate from an inappropriate cross-origin call, a navigation request, or a resource load (such as an image source) where this is not expected.

- Status: `Compliant`
- Implementation: REST endpoints use appropriate HTTP verbs for state-changing operations; controllers define methods with `@PostMapping`, `@PutMapping`, `@DeleteMapping` as appropriate.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/controllers` - controller mappings (`MovieController`, `OrderController`, etc.).
	- `App/src/test/java/com/example/desofs/controller/*IntegrationTests.java` - integration tests asserting expected methods and behaviours.

#### V3.5.4 - Verify that separate applications are hosted on different hostnames to leverage the restrictions provided by same-origin policy, including how documents or scripts loaded by one origin can interact with resources from another origin and hostname-based restrictions on cookies.

- Status: `Not Applicable`
- Implementation: single-application repository; hosting and multi-app deployment are environment concerns outside the application code.
- Evidence / Tests:
	- Deployment notes and infra are out-of-scope for repository-level traceability.

## V4 - API and Web Service

### V4.1 - Generic Web Service Security

#### V4.1.1 - Verify that every HTTP response with a message body contains a Content-Type header field that matches the actual content of the response, including the charset parameter to specify safe character encoding (e.g., UTF-8, ISO-8859-1) according to IANA Media Types, such as "text/", "/+xml" and "/xml".

- Status: `Compliant`
- Implementation: Spring MVC and Jackson set `Content-Type` for JSON responses automatically (`application/json; charset=UTF-8`) at the controller level. Controllers and integration tests assert content-type on responses.
- Evidence / Tests:
	- `App/src/test/java/com/example/desofs/controller/MovieControllerIntegrationTests.java` - asserts `Content-Type: application/json` for API responses.
	- `App/src/test/java/com/example/desofs/controller/*IntegrationTests.java` - other controller integration tests validate response content types.

#### V4.1.2 - Verify that only user-facing endpoints (intended for manual web-browser access) automatically redirect from HTTP to HTTPS, while other services or endpoints do not implement transparent redirects. This is to avoid a situation where a client is erroneously sending unencrypted HTTP requests, but since the requests are being automatically redirected to HTTPS, the leakage of sensitive data goes undiscovered.

- Status: `Partial`
- Implementation: HSTS policy is enforced via `SecurityHeadersFilter` which instructs browsers to use HTTPS. Actual HTTP→HTTPS redirects are typically provided by infrastructure (reverse proxy/load balancer) or container configuration rather than application code. Application-level redirecting is not present across all endpoints.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/security/SecurityHeadersFilter.java` - HSTS header implementation.
	- `App/src/test/java/com/example/desofs/security/SecurityHeadersFilterTest.java` - verifies HSTS header presence.
	- Deployment/config files (Tomcat / proxy) expected to implement redirects; no global app-level redirect tests present.

#### V4.1.3 - Verify that any HTTP header field used by the application and set by an intermediary layer, such as a load balancer, a web proxy, or a backend-for-frontend service, cannot be overridden by the end-user. Example headers might include X-Real-IP, X-Forwarded-*, or X-User-ID.

- Status: `Partial`
- Implementation: the application relies on container and framework filters (e.g., `ForwardedHeaderFilter` / proxy configuration) to process forwarded headers safely. Application code does not assume untrusted client-controlled headers are authoritative; additional validation is recommended in fronting infrastructure.
- Evidence / Tests:
	- `App/src/test/java/com/example/desofs/config/TomcatConfigTest.txt` - tests related to container configuration.
	- CI SAST/static analysis reports would flag misuse of forwarded headers if present.

#### V4.1.4 - Verify that only HTTP methods that are explicitly supported by the application or its API (including OPTIONS during preflight requests) can be used and that unused methods are blocked.

- Status: `Compliant`
- Implementation: controllers declare explicit request mappings (`@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`) and the framework returns 405/404 for unsupported methods. Integration tests exercise expected HTTP verbs.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/controllers` - controller mappings (`MovieController`, `OrderController`, etc.).
	- `App/src/test/java/com/example/desofs/controller/*IntegrationTests.java` - tests asserting correct methods and response codes for unsupported methods.

### V4.2 - HTTP Message Structure Validation

#### V4.2.1 - Verify that all application components (including load balancers, firewalls, and application servers) determine boundaries of incoming HTTP messages using the appropriate mechanism for the HTTP version to prevent HTTP request smuggling. In HTTP/1.x, if a Transfer-Encoding header field is present, the Content-Length header must be ignored per RFC 2616. When using HTTP/2 or HTTP/3, if a Content-Length header field is present, the receiver must ensure that it is consistent with the length of the DATA frames.

- Status: `Partial`
- Implementation: request framing and low-level HTTP parsing are responsibilities of the HTTP server (Tomcat/undercat) and infrastructure. The application does not implement low-level HTTP parsing; deployments should ensure up-to-date server/proxy configurations to mitigate request-smuggling.
- Evidence / Tests:
	- Infrastructure responsibility: Tomcat / proxy configuration and CVE management.
	- No application-level unit tests; CI SAST and deployment hardening recommended.

#### V4.2.2 - Verify that when generating HTTP messages, the Content-Length header field does not conflict with the length of the content as determined by the framing of the HTTP protocol, in order to prevent request smuggling attacks.

- Status: `Partial`
- Implementation: framework (Spring / servlet container) constructs `Content-Length` correctly for responses; application code does not manually set conflicting values.
- Evidence / Tests:
	- Framework behaviour (Spring MVC / Tomcat) and absence of manual `Content-Length` manipulation in code.
	- No dedicated unit tests; rely on infrastructure correctness.

#### V4.2.3 - Verify that the application does not send nor accept HTTP/2 or HTTP/3 messages with connection-specific header fields such as Transfer-Encoding to prevent response splitting and header injection attacks.

- Status: `Partial`
- Implementation: the server/framework enforces HTTP/2/3 semantics and header handling. The application avoids direct manipulation of connection-specific headers.
- Evidence / Tests:
	- Codebase review: no manual `Transfer-Encoding` or connection-specific header usage detected.
	- Recommend infrastructure and server configuration validation.

#### V4.2.4 - Verify that the application only accepts HTTP/2 and HTTP/3 requests where the header fields and values do not contain any CR (\r), LF (\n), or CRLF (\r\n) sequences, to prevent header injection attacks.

- Status: `Partial`
- Implementation: header validation and CRLF protection are enforced by the HTTP server and framework; application-level checks are not required unless custom header parsing exists.
- Evidence / Tests:
	- No custom header parsing found in the codebase.
	- Recommend server-level tests and proxy hardening.

#### V4.2.5 - Verify that, if the application (backend or frontend) builds and sends requests, it uses validation, sanitization, or other mechanisms to avoid creating URIs (such as for API calls) or HTTP request header fields (such as Authorization or Cookie), which are too long to be accepted by the receiving component. This could cause a denial of service, such as when sending an overly long request (e.g., a long cookie header field), which results in the server always responding with an error status.

- Status: `Partial`
- Implementation: application does not construct overly long cookies or header fields; session state is not stored in cookies. There are no explicit size checks in the application; this is mainly enforced by clients/infrastructure.
- Evidence / Tests:
	- Codebase review: no logic that concatenates unbounded header values.
	- No unit tests asserting header length limits; recommend adding tests if required by policy.
	
## V5 - File Handling

### V5.3 - File Storage

#### V5.3.2 - Verify that when the application creates file paths for file operations, instead of user-submitted filenames, it uses internally generated or trusted data, or if user-submitted filenames or file metadata must be used, strict validation and sanitization must be applied. This is to protect against path traversal, local or remote file inclusion (LFI, RFI), and server-side request forgery (SSRF) attacks.

- Status: `Compliant`
- Implementation: file write operations use `ReceiptFileService` which sanitizes user-supplied receipt names and constructs safe file paths. The service avoids direct use of raw user input when resolving filesystem paths.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/services/ReceiptFileService.java` - `sanitizeReceiptName()` and safe path creation logic.
	- `App/src/test/java/com/example/desofs/services/ReceiptFileServiceTest.java` - unit tests for path traversal, null bytes and allowed-character enforcement.

## V6 - Authentication

### V6.1 - Authentication Documentation

#### V6.1.1 - Verify that application documentation defines how controls such as rate limiting, anti-automation, and adaptive response, are used to defend against attacks such as credential stuffing and password brute force. The documentation must make clear how these controls are configured and prevent malicious account lockout.

- Status: `Partial`
- Implementation: deployment and README include high-level guidance on Auth0 and operational controls; `RateLimitFilter` implements per-request throttling at the application layer. A consolidated operational playbook describing account lockout and adaptive response is not present in the repository.
- Evidence / Tests:
	- `App/README.md` - Auth0 configuration and notes on authentication.
	- `App/src/main/java/com/example/desofs/security/RateLimitFilter.java` - rate-limiting implementation.
	- `App/src/test/java/com/example/desofs/security/RateLimitFilterTest.java` and `App/src/test/java/com/example/desofs/config/SecurityConfigIntegrationTest.java` - tests demonstrating rate-limit and header behaviour.

#### V6.1.3 - Verify that, if the application includes multiple authentication pathways, these are all documented together with the security controls and authentication strength which must be consistently enforced across them.

- Status: `Not Applicable`
- Implementation: the application delegates authentication to a single external provider (Auth0) via OAuth2/JWT; there are no multiple internal authentication pathways to harmonise.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/config/Auth0Config.java` - Auth0 configuration.
	- `App/README.md` - Auth0 usage documented.

### V6.2 - Password Security

#### V6.2.1 - Verify that user set passwords are at least 8 characters in length although a minimum of 15 characters is strongly recommended.

- Status: `Not Applicable`
- Implementation: password storage and policies are managed by the external identity provider (Auth0); the application does not implement or enforce password rules directly.
- Evidence / Tests:
	- `App/README.md` and `App/src/main/java/com/example/desofs/config/Auth0Config.java` - note that Auth0 handles authentication responsibilities.

#### V6.2.2 - Verify that users can change their password.

- Status: `Not Applicable`
- Implementation: password change flows are provided by the identity provider (Auth0), not implemented in-application.
- Evidence / Tests:
	- `App/README.md` - external identity provider configuration.

#### V6.2.3 - Verify that password change functionality requires the user's current and new password.

- Status: `Not Applicable`
- Implementation: handled by Auth0 (external service).
- Evidence / Tests:
	- `App/README.md` - delegation to Auth0 noted.

#### V6.2.4 - Verify that passwords submitted during account registration or password change are checked against an available set of, at least, the top 3000 passwords which match the application's password policy, e.g. minimum length.

- Status: `Not Applicable`
- Implementation: password strength/breach checks are the responsibility of Auth0 when used as the identity provider; the application does not perform these checks.
- Evidence / Tests:
	- `App/README.md` - Auth0 integration described; no app-level password checks present.

#### V6.2.5 - Verify that passwords of any composition can be used, without rules limiting the type of characters permitted. There must be no requirement for a minimum number of upper or lower case characters, numbers, or special characters.

- Status: `Not Applicable`
- Implementation: password composition rules are managed by Auth0.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/config/Auth0Config.java` - externalised auth responsibilities.

#### V6.2.6 - Verify that password input fields use type=password to mask the entry. Applications may allow the user to temporarily view the entire masked password, or the last typed character of the password.

- Status: `Not Applicable`
- Implementation: the application does not render a login UI; authentication is delegated to Auth0-hosted pages or client applications.
- Evidence / Tests:
	- `App/README.md` - describes delegation to Auth0.

#### V6.2.7 - Verify that "paste" functionality, browser password helpers, and external password managers are permitted.

- Status: `Not Applicable`
- Implementation: UI-level behavior is outside the scope of this backend service; identity provider should manage UX policies.
- Evidence / Tests:
	- N/A (delegated to Auth0/client applications).

#### V6.2.8 - Verify that the application verifies the user's password exactly as received from the user, without any modifications such as truncation or case transformation.

- Status: `Not Applicable`
- Implementation: authentication handled by Auth0; the application validates JWTs rather than raw passwords.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/config/SecurityConfig.java` - JWT-based resource server configuration.

#### V6.2.9 - Verify that passwords of at least 64 characters are permitted.

- Status: `Not Applicable`
- Implementation: password policies are enforced by the identity provider (Auth0).
- Evidence / Tests:
	- `App/README.md` - external auth provider used.

#### V6.2.10 - Verify that a user's password stays valid until it is discovered to be compromised or the user rotates it. The application must not require periodic credential rotation.

- Status: `Not Applicable`
- Implementation: rotation and compromise handling are managed by the identity provider; the application doesn't enforce periodic rotation.
- Evidence / Tests:
	- `App/README.md` - Auth0 integration described.

#### V6.2.11 - Verify that the documented list of context specific words is used to prevent easy to guess passwords being created.

- Status: `Not Applicable`
- Implementation: this is an identity-provider concern (Auth0); application delegates credential management.
- Evidence / Tests:
	- N/A.

#### V6.2.12 - Verify that passwords submitted during account registration or password changes are checked against a set of breached passwords.

- Status: `Not Applicable`
- Implementation: breach-checking delegated to Auth0 or external identity provider.
- Evidence / Tests:
	- `App/README.md` - notes external auth delegation.

### V6.3 - General Authentication Security

#### V6.3.1 - Verify that controls to prevent attacks such as credential stuffing and password brute force are implemented according to the application's security documentation.

- Status: `Partial`
- Implementation: `RateLimitFilter` provides a mitigation against automated requests and credential-stuffing attempts at the API layer. Complete account-lockout or adaptive response mechanisms are not implemented in-application and would be provided by the identity provider or operational tooling.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/security/RateLimitFilter.java` - rate-limiting implementation.
	- `App/src/test/java/com/example/desofs/security/RateLimitFilterTest.java` - unit tests demonstrating behaviour.

#### V6.3.2 - Verify that default user accounts (e.g., "root", "admin", or "sa") are not present in the application or are disabled.

- Status: `Compliant`
- Implementation: the application does not embed default administrative accounts in code or configuration; authentication is via external provider and user accounts are managed there.
- Evidence / Tests:
	- Codebase search shows no hard-coded default users; `App/README.md` documents Auth0 usage.

#### V6.3.4 - Verify that, if the application includes multiple authentication pathways, there are no undocumented pathways and that security controls and authentication strength are enforced consistently.

- Status: `Not Applicable`
- Implementation: single authentication pathway (Auth0/OAuth2 resource server). No undocumented authentication pathways were found.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/config/SecurityConfig.java` and `App/src/main/java/com/example/desofs/config/Auth0Config.java` - authentication configuration.

#### V6.3.6 - Verify that email is not used as either a single-factor or multi-factor authentication mechanism.

- Status: `Compliant`
- Implementation: the application delegates authentication and MFA to an external Identity Provider (Auth0). The backend accepts validated JWT access tokens and does not implement email-based authentication or self-managed MFA flows. Any email-based mechanisms (verification or notifications) are used for identification/communication only, not as an authentication factor.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/config/Auth0Config.java` - external IdP configuration.
	- `App/src/main/java/com/example/desofs/config/SecurityConfig.java` - JWT resource-server configuration; app accepts tokens rather than raw credentials.
	- `App/README.md` - notes Auth0 delegation and responsibilities for credential/MFA policies.

#### V6.3.8 - Verify that valid users cannot be deduced from failed authentication challenges, such as by basing on error messages, HTTP response codes, or different response times. Registration and forgot password functionality must also have this protection.

- Status: `Partial`
- Implementation: the API consistently returns HTTP 401 for unauthenticated requests and avoids streaming stack traces in error responses; `GlobalExceptionHandler` centralises error formatting to prevent information leakage. Registration/forgot-password flows are delegated to the IdP and are therefore outside the API surface of this service - proper protection for those flows depends on the IdP configuration. Timing side-channel protections are not implemented in-application (rely on IdP and infra).
- Evidence / Tests:
	- `App/src/test/java/com/example/desofs/config/SecurityConfigIntegrationTest.java` - asserts unauthenticated requests return 401 and that error responses omit stack traces and include a correlation ID.
	- `App/src/test/java/com/example/desofs/GlobalExceptionHandlerTest.java` - tests that error payloads do not expose exception internals (if present in repo).
	- `App/README.md` and `App/src/main/java/com/example/desofs/config/Auth0Config.java` - note delegation of registration/forgot-password to IdP.

### V6.7 - Cryptographic authentication mechanism

#### V6.7.1 - Verify that the certificates used to verify cryptographic authentication assertions are stored in a way protects them from modification.

- Status: `Partial`
- Implementation: JWT signature verification uses public keys obtained from the IdP's JWKS endpoint (configured via `spring.security.oauth2.resourceserver.jwt.issuer-uri` / `Auth0Config`). The application does not store long-lived private keys in the repository. The JWKS keys are retrieved at runtime from the IdP; operational responsibilities (caching, rotation, trust-store protections) are delegated to the runtime environment and Spring Security's `NimbusJwtDecoder` default behaviour. If certificate/key files were to be used, they must be stored in a protected secrets store (not in repo).
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/config/Auth0Config.java` - configuration of issuer/JWKS settings.
	- `App/src/main/java/com/example/desofs/config/SecurityConfig.java` - `JwtDecoder` configuration using audience validation and remote key validation.
	- `App/target/site/jacoco/jacoco.xml` and runtime integration tests demonstrate JWT-based flows exercised in tests (`SecurityConfigIntegrationTest`).

### V6.8 - Authentication with an Identity Provider

#### V6.8.2 - Verify that the presence and integrity of digital signatures on authentication assertions (for example on JWTs or SAML assertions) are always validated, rejecting any assertions that are unsigned or have invalid signatures.

- Status: `Compliant`
- Implementation: the application is configured as an OAuth2 Resource Server using Spring Security; JWT decoding and signature verification are performed by the configured `JwtDecoder` (remote JWKS retrieval from the IdP). The `AudienceValidator` is used to verify audience claims after signature validation. Unsigned or invalidly-signed tokens are rejected by the resource server before reaching controller logic.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/config/SecurityConfig.java` - resource server and `JwtDecoder` configuration.
	- `App/src/main/java/com/example/desofs/config/AudienceValidator.java` - audience checks applied to JWTs.
	- `App/src/test/java/com/example/desofs/config/SecurityConfigIntegrationTest.java` - integration tests exercising authenticated/unauthenticated flows.

#### V6.8.4 - Verify that, if an application uses a separate Identity Provider (IdP) and expects specific authentication strength, methods, or recentness for specific functions, the application verifies this using the information returned by the IdP. For example, if OIDC is used, this might be achieved by validating ID Token claims such as 'acr', 'amr', and 'auth_time' (if present). If the IdP does not provide this information, the application must have a documented fallback approach that assumes that the minimum strength authentication mechanism was used (for example, single-factor authentication using username and password).

- Status: `Partial`
- Implementation: the application currently validates core JWT claims (issuer, audience, expiry) and role claims (`RoleGuard`) but does not explicitly check `acr`, `amr`, or `auth_time` claims. Where higher-assurance authentication is required for specific endpoints, the application must inspect such claims (if provided by the IdP) or rely on the IdP to assert appropriate assurance levels. A documented fallback approach is not present in the repository and should be added to operational/deployment documentation.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/config/AudienceValidator.java` - audience validation implementation.
	- `App/src/main/java/com/example/desofs/security/RoleGuard.java` - role-based checks via JWT claims.
	- `App/src/test/java/com/example/desofs/security/RoleGuardTest.java` and `App/src/test/java/com/example/desofs/config/SecurityConfigIntegrationTest.java` - tests demonstrating JWT claim handling and endpoint protection.
	- Recommendation: add claim-inspection logic in `SecurityConfig` or a custom `AuthenticationManager` to enforce `acr`/`amr`/`auth_time` where required, and document fallback assumptions in `App/README.md` or `development.md`.

## V7 - Session Management

### V7.1 - Session Management Documentation

#### V7.1.1 - Verify that the user's session inactivity timeout and absolute maximum session lifetime are documented, are appropriate in combination with other controls, and that the documentation includes justification for any deviations from NIST SP 800-63B re-authentication requirements.

- Status: `Partial`
- Implementation: the repository documents that the API is stateless and delegates authentication to Auth0; token lifetimes (access/refresh) are controlled by the IdP and not centrally documented in the application docs. There is no consolidated justification for session lifetime choices against NIST SP 800-63B in the repo.
- Evidence / Tests:
	- `App/README.md` - notes JWT usage and Auth0 delegation.
	- `App/src/main/java/com/example/desofs/config/SecurityConfig.java` - stateless session policy and JWT decoder configuration.

#### V7.1.2 - Verify that the documentation defines how many concurrent (parallel) sessions are allowed for one account as well as the intended behaviors and actions to be taken when the maximum number of active sessions is reached.

- Status: `Partial`
- Implementation: concurrent session limits are not enforced by the application; this is expected to be an IdP/operational concern. The repository does not currently document concurrent session policies or handling.
- Evidence / Tests:
	- `App/README.md` - indicates external IdP is used for sessions; no concurrency policy found in repo.

#### V7.1.3 - Verify that all systems that create and manage user sessions as part of a federated identity management ecosystem (such as SSO systems) are documented along with controls to coordinate session lifetimes, termination, and any other conditions that require re-authentication.

- Status: `Partial`
- Implementation: the IdP (Auth0) is documented as the authentication authority, but coordination controls (single-logout, session coordination) are not detailed in repository documentation.
- Evidence / Tests:
	- `App/README.md` and `App/src/main/java/com/example/desofs/config/Auth0Config.java` - IdP configuration present; no SSO coordination docs.

### V7.2 - Fundamental Session Management Security

#### V7.2.1 - Verify that the application performs all session token verification using a trusted, backend service.

- Status: `Compliant`
- Implementation: JWT signature and claim validation are performed server-side using Spring Security's `JwtDecoder` (remote JWKS retrieval) and custom `AudienceValidator` for audience enforcement.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/config/SecurityConfig.java` - `jwtDecoder()` bean with `DelegatingOAuth2TokenValidator`.
	- `App/src/main/java/com/example/desofs/config/AudienceValidator.java` - audience validation implementation.

#### V7.2.2 - Verify that the application uses either self-contained or reference tokens that are dynamically generated for session management, i.e. not using static API secrets and keys.

- Status: `Compliant`
- Implementation: the application relies on self-contained JWT access tokens issued by the IdP; no static API secrets are used for user session tokens.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/config/SecurityConfig.java` - resource-server configuration for JWT.

#### V7.2.4 - Verify that the application generates a new session token on user authentication, including re-authentication, and terminates the current session token.

- Status: `Compliant`
- Implementation: token issuance and rotation are performed by the IdP (Auth0). Application-side termination is enforced by a per-user invalidation cut-off: whenever an administrator changes a user's role, `UserService.assignRole` / `removeRole` calls `TokenInvalidationService.invalidateTokensFor(userId, reason)` which upserts a row in `user_token_invalidations` (Flyway migration `V8__create_user_token_invalidations.sql`) and additionally calls `Auth0ManagementClient.invalidateSessions(userId)` (best-effort `DELETE /api/v2/users/{id}/sessions`). Every subsequent request is intercepted by `TokenFreshnessFilter`, which rejects any JWT whose `iat` claim is older than the stored cut-off with `401 invalid_token`, forcing the client to obtain a new token. See `Deliverables/Phase2/Sprint1/Development/development.md` Cap12.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/services/TokenInvalidationService.java` and `App/src/main/java/com/example/desofs/services/ITokenInvalidationService.java` - denylist write path and contract.
	- `App/src/main/java/com/example/desofs/security/TokenFreshnessFilter.java` - request-time enforcement after `BearerTokenAuthenticationFilter`.
	- `App/src/main/java/com/example/desofs/security/Auth0ManagementClient.java` (method `invalidateSessions`) - IdP session drop.
	- `App/src/main/java/com/example/desofs/services/UserService.java` - invocation on `assignRole` / `removeRole`.
	- `App/src/main/java/com/example/desofs/config/SecurityConfig.java` - filter wiring.
	- `App/src/test/java/com/example/desofs/services/TokenInvalidationServiceTest.java` - 10 unit tests covering upsert idempotency, before/after-cut-off semantics, null-input tolerance.
	- `App/src/test/java/com/example/desofs/security/TokenFreshnessFilterTest.java` - 5 tests covering pass-through and 401 rejection cases.
	- `App/src/test/java/com/example/desofs/security/Auth0ManagementClientTest.java` - `invalidateSessions_*` tests (bearer-authenticated DELETE, error swallowing, input validation).
	- `App/src/test/java/com/example/desofs/services/UserServiceTest.java` - verifies invalidation happens after audit and before/with the Auth0 call, on both role assignment and removal.

### V7.3 - Session Timeout

#### V7.3.1 - Verify that there is an inactivity timeout such that re-authentication is enforced according to risk analysis and documented security decisions.

- Status: `Partial`
- Implementation: inactivity timeouts are expressed via JWT expiry (`exp`) managed by the IdP; the application enforces expiry using Spring Security's timestamp validators in the `JwtDecoder` configuration.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/config/SecurityConfig.java` - `JwtValidators.createDefaultWithIssuer()` usage.
	- `App/src/test/java/com/example/desofs/config/SecurityConfigIntegrationTest.java` - tests that unauthenticated requests are rejected and that authenticated flows proceed.

#### V7.3.2 - Verify that there is an absolute maximum session lifetime such that re-authentication is enforced according to risk analysis and documented security decisions.

- Status: `Partial`
- Implementation: absolute token lifetime is controlled by the IdP; the application relies on enforced expiry and audience/issuer checks but does not maintain server-side absolute-session state.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/config/SecurityConfig.java` - JWT validation configuration.

### V7.4 - Session Termination

#### V7.4.1 - Verify that when session termination is triggered (such as logout or expiration), the application disallows any further use of the session. For reference tokens or stateful sessions, this means invalidating the session data at the application backend. Applications using self-contained tokens will need a solution such as maintaining a list of terminated tokens, disallowing tokens produced before a per-user date and time or rotating a per-user signing key.

- Status: `Compliant`
- Implementation: a **per-user issued-at cut-off** strategy is implemented (one of the three patterns explicitly accepted by V7.4.1 for self-contained tokens). On every administrative role change, `UserService.invalidateUserSessions(targetUserId, reason)` (1) upserts a row in `user_token_invalidations` containing the cut-off `Instant.now()` and (2) issues a best-effort `DELETE /api/v2/users/{id}/sessions` against the Auth0 Management API. On every subsequent request, `TokenFreshnessFilter` (registered after `BearerTokenAuthenticationFilter`) compares the JWT's `iat` claim against the stored cut-off and returns `401 Unauthorized` with `WWW-Authenticate: Bearer error="invalid_token"` if the token was issued before the cut-off. The denylist is the authoritative control; the Auth0 call is defence-in-depth that drops the IdP-side SSO session so silent refresh fails. Full design rationale, threat analysis and trade-offs are documented in `Deliverables/Phase2/Sprint1/Development/development.md` Cap12.
- Evidence / Tests:
	- `App/src/main/resources/db/migration/V8__create_user_token_invalidations.sql` - schema (PK on `auth0_user_id`, millisecond-precision `invalidated_after`).
	- `App/src/main/java/com/example/desofs/domain/UserTokenInvalidation.java` and `App/src/main/java/com/example/desofs/repositories/UserTokenInvalidationRepository.java` - entity and repository.
	- `App/src/main/java/com/example/desofs/services/TokenInvalidationService.java` - idempotent upsert + freshness check.
	- `App/src/main/java/com/example/desofs/security/TokenFreshnessFilter.java` - request-time enforcement returning RFC 6750-compliant 401.
	- `App/src/main/java/com/example/desofs/security/Auth0ManagementClient.java` - `invalidateSessions` (best-effort IdP session drop).
	- `App/src/main/java/com/example/desofs/services/UserService.java` - hook on `assignRole` and `removeRole` (writes denylist before contacting Auth0).
	- `App/src/main/java/com/example/desofs/config/SecurityConfig.java` - filter chain ordering (`BearerTokenAuthenticationFilter` → `TokenFreshnessFilter` → `RateLimitFilter`).
	- `App/src/test/java/com/example/desofs/services/TokenInvalidationServiceTest.java` (10 tests), `App/src/test/java/com/example/desofs/security/TokenFreshnessFilterTest.java` (5 tests), `App/src/test/java/com/example/desofs/security/Auth0ManagementClientTest.java` (`invalidateSessions_*` tests), `App/src/test/java/com/example/desofs/services/UserServiceTest.java` (call-order verification).

### V7.5 - Defenses Against Session Abuse

#### V7.5.3 - Verify that the application requires further authentication with at least one factor or secondary verification before performing highly sensitive transactions or operations.

- Status: `Partial`
- Implementation: role-based access control is enforced (`RoleGuard`) to restrict access to privileged operations, but explicit step-up authentication (MFA re-prompt / checking `auth_time`/`acr` claims) is not implemented in-application. For high-risk operations, claim-inspection or IdP-driven step-up should be used.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/security/RoleGuard.java` and `App/src/test/java/com/example/desofs/security/RoleGuardTest.java` - role enforcement tests.

### V7.6 - Federated Re-authentication

#### V7.6.1 - Verify that session lifetime and termination between Relying Parties (RPs) and Identity Providers (IdPs) behave as documented, requiring re-authentication as necessary such as when the maximum time between IdP authentication events is reached.

- Status: `Partial`
- Implementation: the repo documents the IdP boundary but does not include SSO/session coordination policies (e.g., single logout, IdP-initiated session termination). These controls must be designed in the deployment/IdP configuration.
- Evidence / Tests:
	- `App/README.md` and `App/src/main/java/com/example/desofs/config/Auth0Config.java` - IdP configuration present; no SSO coordination tests in repo.

#### V7.6.2 - Verify that creation of a session requires either the user's consent or an explicit action, preventing the creation of new application sessions without user interaction.

- Status: `Compliant`
- Implementation: session creation (token issuance) is performed by the IdP after explicit authentication/consent flows; the application does not silently create sessions for users.
- Evidence / Tests:
	- `App/README.md` and IdP configuration notes.

## V8 - Authorization

### V8.1 - Authorization Documentation

#### V8.1.1 - Verify that authorization documentation defines rules for restricting function-level and data-specific access based on consumer permissions and resource attributes.

- Status: `Partial`
- Implementation: high-level authorization approach is documented (JWT bearer tokens + role claims via Auth0) but fine-grained mapping of roles to functions/data and a full permissions matrix are not present in the repository. The codebase implements role-based checks via `RoleGuard` and enforces checks at controller entry points.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/security/RoleGuard.java` - role claim enforcement implementation.
	- Controllers: `App/src/main/java/com/example/desofs/controllers/OrderController.java`, `App/src/main/java/com/example/desofs/controllers/MovieController.java` - call `roleGuard.requireRole(...)` to protect endpoints.
	- `App/src/test/java/com/example/desofs/security/RoleGuardTest.java` - unit tests for role enforcement.

#### V8.1.2 - Verify that the documentation describes the processes for granting, revoking and reviewing privileged roles.

- Status: `Partial`
- Implementation: no in-repo administrative tooling or documented processes for role lifecycle (grant/revoke/review). Role management is expected to be performed in the IdP (Auth0); the repo should include an administrative playbook or links to the organisation's IdP role-management procedures.
- Evidence / Tests:
	- `App/README.md` - Auth0 is referenced as the identity provider; no admin role lifecycle docs found.

### V8.2 - Access Control Implementation

#### V8.2.1 - Verify that the application enforces least privilege and denies access by default.

- Status: `Compliant`
- Implementation: default-deny policy is enforced by `SecurityConfig` (`anyRequest().authenticated()`) and method/controller-level guards enforce role checks via `RoleGuard`. Unauthenticated requests are rejected by the resource server configuration.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/config/SecurityConfig.java` - `anyRequest().authenticated()` and OAuth2 resource-server setup.
	- `App/src/main/java/com/example/desofs/controllers/*Controller.java` - endpoints protected and call `roleGuard.requireRole()` where necessary.
	- `App/src/test/java/com/example/desofs/config/SecurityConfigIntegrationTest.java` - asserts unauthenticated requests return 401.

#### V8.2.2 - Verify that authorization checks are applied as close as possible to the resource and use robust identity information (claims) rather than user-supplied inputs.

- Status: `Compliant`
- Implementation: controllers receive the authenticated `Jwt` principal and call `RoleGuard.requireRole(jwt, Role.X)` which reads roles from a configured claim namespace; authorization decisions are made using JWT claims, not client parameters.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/controllers/OrderController.java` - `@AuthenticationPrincipal Jwt jwt` and `roleGuard.requireRole(...)` usage.
	- `App/src/main/java/com/example/desofs/security/RoleGuard.java` and `App/src/test/java/com/example/desofs/security/RoleGuardTest.java`.

#### V8.2.3 - Verify that access control is enforced consistently across code paths, including APIs, background jobs, and messaging consumers.

- Status: `Partial`
- Implementation: API endpoints enforce role checks; background jobs and messaging consumers (if present) should also perform equivalent checks but are not represented in the repository as protected components. A sweep to ensure all non-HTTP entrypoints apply the same guards is recommended.
- Evidence / Tests:
	- Controller code and tests demonstrate API-level enforcement; no messaging/background consumer examples found.

### V8.3 - Privilege Management and Separation

#### V8.3.1 - Verify that privileged roles and administrative functions are restricted and logged.

- Status: `Partial`
- Implementation: privileged functions are protected by role checks (e.g., `Role.ADMIN` in `MovieController.create`). Logging exists in controllers (`logger.info`) but dedicated audit logs for privileged actions are not yet centralised; recommend adding structured audit events for admin actions.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/controllers/MovieController.java` - `roleGuard.requireRole(jwt, Role.ADMIN)`.
	- `App/src/test/java/com/example/desofs/controller/*IntegrationTests.java` - integration tests for controller behaviours.

#### V8.3.2 - Verify separation of duties for critical functions where feasible.

- Status: `Partial`
- Implementation: role model includes distinct roles (CUSTOMER, ADMIN, SUPPORT) enforced by `RoleGuard`; explicit separation of duties rules and cross-checks are not codified in the repo and should be documented as policy where required.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/domain/Role.java` and `RoleGuard` enforcement.

### V8.4 - Delegation and Claims

#### V8.4.1 - Verify that delegated authorization (via claims/assertions from an IdP) is validated and not trusted blindly.

- Status: `Compliant`
- Implementation: JWT signature and issuer/audience validation are enforced via `JwtDecoder` configuration; `RoleGuard` reads roles from a namespaced claim. The application validates tokens rather than accepting unsigned assertions.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/config/SecurityConfig.java` and `App/src/main/java/com/example/desofs/config/AudienceValidator.java`.

#### V8.4.2 - Verify that claim sources are documented, validated and namespaced to avoid collisions.

- Status: `Compliant`
- Implementation: roles are expected under a configurable namespaced claim (property `emovieshop.auth0.roles-claim`) and `RoleGuard` reads it; this prevents accidental collisions with standard JWT claims.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/security/RoleGuard.java` and `App/src/test/java/com/example/desofs/security/RoleGuardTest.java`.

## V9 - Self-contained Tokens

### V9.1 - Token source and integrity

#### V9.1.1 - Verify that self-contained tokens are validated using their digital signature or MAC to protect against tampering before accepting the token's contents.

- Status: `Compliant`
- Implementation: the application is configured as an OAuth2 Resource Server and uses a `JwtDecoder` (`NimbusJwtDecoder` via `JwtDecoders.fromIssuerLocation(issuerUri)`) which validates token signatures using the IdP's JWKS. Tokens are rejected by the framework if signature validation fails before controller logic is invoked.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/config/SecurityConfig.java` - `jwtDecoder()` bean using `JwtDecoders.fromIssuerLocation`.
	- `App/src/test/java/com/example/desofs/config/SecurityConfigIntegrationTest.java` - integration tests exercising authenticated/unauthenticated flows.

#### V9.1.2 - Verify that only algorithms on an allowlist can be used to create and verify self-contained tokens, for a given context. The allowlist must include the permitted algorithms, ideally only either symmetric or asymmetric algorithms, and must not include the 'None' algorithm. If both symmetric and asymmetric must be supported, additional controls will be needed to prevent key confusion.

- Status: `Partial`
- Implementation: algorithm selection is predominantly controlled by the IdP and the JWKS published by the issuer. The `NimbusJwtDecoder` will use the algorithm specified in the JWK; the application does not currently maintain an explicit allowlist of JWS algorithms in code. To harden, the application can constrain accepted algorithms by configuring a `JWSAlgorithm` allowlist or by using a custom `JwtDecoder` that enforces permitted algorithms.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/config/SecurityConfig.java` - default decoder construction; no explicit algorithm allowlist present.
	- Recommendation: add algorithm allowlist configuration in `SecurityConfig` or validate `alg` in a custom validator.

#### V9.1.3 - Verify that key material that is used to validate self-contained tokens is from trusted pre-configured sources for the token issuer, preventing attackers from specifying untrusted sources and keys. For JWTs and other JWS structures, headers such as 'jku', 'x5u', and 'jwk' must be validated against an allowlist of trusted sources.

- Status: `Compliant`
- Implementation: the application retrieves JWKS from the configured issuer URI (`issuerUri`) rather than trusting per-token key URLs; `JwtDecoders.fromIssuerLocation` obtains keys from the IdP's well-known JWKs endpoint, preventing token-supplied key locations from being trusted. There is no code path that accepts arbitrary `jku`/`jwk` headers as trusted key sources.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/config/SecurityConfig.java` - `jwtDecoder()` built from issuer location.
	- `App/src/test/java/com/example/desofs/config/SecurityConfigIntegrationTest.java` - tests exercising valid token acceptance.

### V9.2 - Token content

#### V9.2.1 - Verify that, if a validity time span is present in the token data, the token and its content are accepted only if the verification time is within this validity time span. For example, for JWTs, the claims 'nbf' and 'exp' must be verified.

- Status: `Compliant`
- Implementation: `JwtValidators.createDefaultWithIssuer(issuerUri)` is used to validate issuer and timestamp claims; the `JwtDecoder` enforces `exp`/`nbf` (with clock-skew handling) before tokens are accepted.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/config/SecurityConfig.java` - `jwtDecoder()` configures default validators including timestamp checks.
	- `App/src/test/java/com/example/desofs/config/SecurityConfigIntegrationTest.java` - integration tests for authenticated flows and rejection of unauthenticated requests.

#### V9.2.2 - Verify that the service receiving a token validates the token to be the correct type and is meant for the intended purpose before accepting the token's contents. For example, only access tokens can be accepted for authorization decisions and only ID Tokens can be used for proving user authentication.

- Status: `Partial`
- Implementation: the application validates audience and issuer and relies on the IdP to issue appropriate token types. There is no explicit enforcement of token type (`typ` or `token_use`) in code; where required, additional checks should be implemented to verify token intent (e.g., check `token_use` claim or `typ` header) to ensure only access tokens are used for authorization.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/config/SecurityConfig.java` and `App/src/main/java/com/example/desofs/config/AudienceValidator.java` - audience/issuer validation present.
	- Recommendation: add explicit token-type checks if IdP issues multiple token types to the same audience.

#### V9.2.3 - Verify that the service only accepts tokens which are intended for use with that service (audience). For JWTs, this can be achieved by validating the 'aud' claim against an allowlist defined in the service.

- Status: `Compliant`
- Implementation: `AudienceValidator` checks the `aud` claim against the configured audience(s); this prevents tokens issued for other audiences from being accepted.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/config/AudienceValidator.java` - audience check implementation.
	- `App/src/test/java/com/example/desofs/config/AudienceValidatorTest.java` - unit tests validating audience behaviour.

#### V9.2.4 - Verify that, if a token issuer uses the same private key for issuing tokens to different audiences, the issued tokens contain an audience restriction that uniquely identifies the intended audiences. This will prevent a token from being reused with an unintended audience. If the audience identifier is dynamically provisioned, the token issuer must validate these audiences in order to make sure that they do not result in audience impersonation.

- Status: `Partial`
- Implementation: the application enforces audience checks but cannot control an issuer's choice to reuse signing keys. The service defends by validating `aud` and `iss` claims; to further reduce risk, use short-lived tokens, token binding, or audience-scoped keys at the IdP. The repository should document required IdP practices (audience restrictions and key management) as part of the deployment/integration guidance.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/config/AudienceValidator.java` and `App/src/main/java/com/example/desofs/config/SecurityConfig.java` - show audience and issuer validation.
	- Recommendation: document IdP key/audience practices and, where possible, require audience-scoped signing or enforce additional token binding mechanisms.

## V10 - OAuth and OIDC

### V10.1 - Generic OAuth and OIDC Security

#### V10.1.1 - Verify that tokens are only sent to components that strictly need them. For example, when using a backend-for-frontend pattern for browser-based JavaScript applications, access and refresh tokens shall only be accessible for the backend.

- Status: `Partial`
- Implementation: the API expects bearer access tokens in the `Authorization` header and does not handle or expose refresh tokens. Guidance for client-side token storage (BFF vs SPA) is not enforced by the repo; this is a client/IdP integration concern and should be documented in `App/README.md`.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/config/SecurityConfig.java` - resource-server expects bearer tokens.
	- `App/README.md` - Auth0 integration notes; recommend adding explicit client guidance.

#### V10.1.2 - Verify that the client only accepts values from the authorization server (such as the authorization code or ID Token) if these values result from an authorization flow that was initiated by the same user agent session and transaction. This requires that client-generated secrets, such as the proof key for code exchange (PKCE) 'code_verifier', 'state' or OIDC 'nonce', are not guessable, are specific to the transaction, and are securely bound to both the client and the user agent session in which the transaction was started.

- Status: `Not Applicable`
- Implementation: client-side authorization flows (PKCE, nonce, state) are out-of-scope for this backend service. The application relies on the IdP (Auth0) and client apps to correctly implement these protections.
- Evidence / Tests:
	- `App/README.md` - indicates external IdP and client responsibilities; no client-side code in this repo.

### V10.3 - OAuth Resource Server

#### V10.3.1 -Verify that the resource server only accepts access tokens that are intended for use with that service (audience). The audience may be included in a structured access token (such as the 'aud' claim in JWT), or it can be checked using the token introspection endpoint.

- Status: `Compliant`
- Implementation: `AudienceValidator` enforces that the `aud` claim contains the configured audience(s); tokens with mismatched audiences are rejected by the `JwtDecoder` validator chain.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/config/AudienceValidator.java` and `App/src/main/java/com/example/desofs/config/SecurityConfig.java` - audience validation in `jwtDecoder()`.
	- `App/src/test/java/com/example/desofs/config/AudienceValidatorTest.java` - unit tests.

#### V10.3.2 - Verify that the resource server enforces authorization decisions based on claims from the access token that define delegated authorization. If claims such as 'sub', 'scope', and 'authorization_details' are present, they must be part of the decision.

- Status: `Compliant`
- Implementation: controllers receive the authenticated `Jwt` principal and use claims (e.g., `sub`, namespaced `roles`) for authorization decisions via `RoleGuard`. `scope`/`authorization_details` are available in the token if provided by the IdP and can be used by `RoleGuard` or additional validators.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/security/RoleGuard.java` and controller usages (`OrderController`, `MovieController`).
	- `App/src/test/java/com/example/desofs/security/RoleGuardTest.java`.

#### V10.3.3 - Verify that if an access control decision requires identifying a unique user from an access token (JWT or related token introspection response), the resource server identifies the user from claims that cannot be reassigned to other users. Typically, it means using a combination of 'iss' and 'sub' claims.

- Status: `Compliant`
- Implementation: controllers use `Jwt.getSubject()` (`sub`) and the `iss` is validated by the `JwtDecoder` setup, ensuring `iss`+`sub` uniquely identify a principal.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/controllers/OrderController.java` - uses `jwt.getSubject()`.
	- `App/src/main/java/com/example/desofs/config/SecurityConfig.java` - `JwtValidators.createDefaultWithIssuer(issuerUri)`.

### V10.4 - OAuth Authorization Server

#### V10.4.1 - Verify that the authorization server validates redirect URIs based on a client-specific allowlist of pre-registered URIs using exact string comparison.

- Status: `Not Applicable`
- Implementation: authorization server behaviour (redirect URI validation, code issuance, PKCE) is the responsibility of the IdP (Auth0) and not implemented in this service.
- Evidence / Tests:
	- `App/README.md` - notes Auth0; no authorization server code in repo.

#### V10.4.2 - Verify that, if the authorization server returns the authorization code in the authorization response, it can be used only once for a token request. For the second valid request with an authorization code that has already been used to issue an access token, the authorization server must reject a token request and revoke any issued tokens related to the authorization code.

- Status: `Not Applicable`
- Implementation: Authorization server replay/one-time-code protections are handled by the IdP (Auth0).

#### V10.4.3 - Verify that the authorization code is short-lived. The maximum lifetime can be up to 10 minutes for L1 and L2 applications and up to 1 minute for L3 applications.

- Status: `Not Applicable`
- Implementation: code expiration is enforced by the authorization server.

#### V10.4.4 - Verify that for a given client, the authorization server only allows the usage of grants that this client needs to use. Note that the grants 'token' (Implicit flow) and 'password' (Resource Owner Password Credentials flow) must no longer be used.

- Status: `Not Applicable`

#### V10.4.5 - Verify that the authorization server mitigates refresh token replay attacks for public clients, preferably using sender-constrained refresh tokens, i.e., Demonstrating Proof of Possession (DPoP) or Certificate-Bound Access Tokens using mutual TLS (mTLS). For L1 and L2 applications, refresh token rotation may be used. If refresh token rotation is used, the authorization server must invalidate the refresh token after usage, and revoke all refresh tokens for that authorization if an already used and invalidated refresh token is provided.

- Status: `Not Applicable`

#### V10.4.6 - Verify that, if the code grant is used, the authorization server mitigates authorization code interception attacks by requiring proof key for code exchange (PKCE). For authorization requests, the authorization server must require a valid 'code_challenge' value and must not accept a 'code_challenge_method' value of 'plain'. For a token request, it must require validation of the 'code_verifier' parameter.

- Status: `Not Applicable`
- Implementation: PKCE enforcement is an IdP capability and must be configured at the authorization server.

#### V10.4.8 - Verify that refresh tokens have an absolute expiration, including if sliding refresh token expiration is applied.

- Status: `Not Applicable`

#### V10.4.9 - Verify that refresh tokens and reference access tokens can be revoked by an authorized user using the authorization server user interface, to mitigate the risk of malicious clients or stolen tokens.

- Status: `Not Applicable`

#### V10.4.10 - Verify that confidential client is authenticated for client-to-authorized server backchannel requests such as token requests, pushed authorization requests (PAR), and token revocation requests.

- Status: `Not Applicable`

#### V10.4.11 - Verify that the authorization server configuration only assigns the required scopes to the OAuth client.

- Status: `Not Applicable`
- Implementation: scope assignment and client configuration are IdP-side responsibilities (Auth0). The application should request and validate only the scopes it needs but does not control server-side client scope assignment.

### V10.5 - OIDC Client

#### V10.5.2 - Verify that the client uniquely identifies the user from ID Token claims, usually the 'sub' claim, which cannot be reassigned to other users (for the scope of an identity provider).

- Status: `Compliant` (for resource-server behavior)
- Implementation: server-side code uses `sub` claim as the user identifier for actions (e.g., `jwt.getSubject()` in controllers). Client-side OIDC flows are out of scope for this repo.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/controllers/OrderController.java` - `String auth0Id = jwt.getSubject();` used as user identity.

### V10.6 - OpenID Provider

#### V10.6.1 - Verify that the OpenID Provider only allows values 'code', 'ciba', 'id_token', or 'id_token code' for response mode. Note that 'code' is preferred over 'id_token code' (the OIDC Hybrid flow), and 'token' (any Implicit flow) must not be used.

- Status: `Not Applicable`
- Implementation: OpenID Provider behaviour is delegated to Auth0; the application does not act as an OP.

#### V10.6.2 - Verify that the OpenID Provider mitigates denial of service through forced logout. By obtaining explicit confirmation from the end-user or, if present, validating parameters in the logout request (initiated by the relying party), such as the 'id_token_hint'.

- Status: `Not Applicable`
- Implementation: delegated to IdP.

### V10.7 - Consent Management

#### V10.7.1 - Verify that the authorization server ensures that the user consents to each authorization request. If the identity of the client cannot be assured, the authorization server must always explicitly prompt the user for consent.

- Status: `Not Applicable`
- Implementation: consent UI and flows are provided by the IdP (Auth0) and by client applications; not implemented in this backend repo.

#### V10.7.2 - Verify that when the authorization server prompts for user consent, it presents sufficient and clear information about what is being consented to. When applicable, this should include the nature of the requested authorizations (typically based on scope, resource server, Rich Authorization Requests (RAR) authorization details), the identity of the authorized application, and the lifetime of these authorizations.

- Status: `Not Applicable`
- Implementation: delegated to the IdP (Auth0) and to client applications.

## V11 - Cryptographic Inventory and Documentation

### V11.1 - Cryptographic Inventory and Documentation

#### V11.1.1 - Verify that there is a documented policy for management of cryptographic keys and a cryptographic key lifecycle that follows a key management standard such as NIST SP 800-57. This should include ensuring that keys are not overshared (for example, with more than two entities for shared secrets and more than one entity for private keys).

- Status: `Partial`
- Implementation: there is no central cryptographic key management policy or lifecycle doc in the repository. The application avoids storing private keys or long-lived secrets in source control and delegates signing/verification responsibilities to the IdP (Auth0) for JWTs. For other secrets (database credentials, JWKS URIs) the repo uses environment-backed configuration (`application-dev.properties` references environment variables).
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/config/Auth0Config.java` - configuration surface for issuer/JWKS location.
	- `App/target/classes/application-dev.properties` and `App/src/test/resources/application-test.properties` - show environment-driven configuration for secrets (DB credentials, issuer URI).
	- Recommendation: add a documented key lifecycle policy (rotate, retire, compromise handling) to the repository's security documentation.

#### V11.1.2 - Verify that a cryptographic inventory is performed, maintained, regularly updated, and includes all cryptographic keys, algorithms, and certificates used by the application. It must also document where keys can and cannot be used in the system, and the types of data that can and cannot be protected using the keys.

- Status: `Partial`
- Implementation: there is no dedicated cryptographic inventory file in the repo. Cryptographic usage is minimal in-application: JWT verification (via IdP JWKS) and standard TLS for network transports are assumed. Inventory of keystores/certificates and algorithm lists should be added to deployment documentation.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/config/SecurityConfig.java` and `App/src/main/java/com/example/desofs/config/Auth0Config.java` - demonstrate cryptographic responsibilities (JWT signature verification via JWKS).
	- Recommendation: produce a `crypto-inventory.md` listing keys/JWKS endpoints, expected algorithms and trust boundaries.

### V11.2 - Secure Cryptography Implementation

#### V11.2.1 - Verify that industry-validated implementations (including libraries and hardware-accelerated implementations) are used for cryptographic operations.

- Status: `Compliant`
- Implementation: cryptographic operations for token signature validation and JWT parsing are delegated to Spring Security / Nimbus libraries (`NimbusJwtDecoder`) which are industry-standard implementations. TLS and underlying JVM crypto providers handle transport-level cryptography.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/config/SecurityConfig.java` - uses `JwtDecoders.fromIssuerLocation` which uses Nimbus under the hood.
	- Dependency management: Maven `pom.xml` declares Spring Security OAuth2 Resource Server dependencies (used by `JwtDecoder`).

#### V11.2.2 - Verify that the application is designed with crypto agility such that random number, authenticated encryption, MAC, or hashing algorithms, key lengths, rounds, ciphers and modes can be reconfigured, upgraded, or swapped at any time, to protect against cryptographic breaks. Similarly, it must also be possible to replace keys and passwords and re-encrypt data. This will allow for seamless upgrades to post-quantum cryptography (PQC), once high-assurance implementations of approved PQC schemes or standards are widely available.

- Status: `Partial`
- Implementation: the application delegates most cryptographic choices to external components (IdP, JVM crypto providers). `SecurityConfig` uses external JWKS and validators, which allows key rotation at the IdP without code changes. However, there is no explicit application-level configuration for algorithm allowlists or PQC migration plans.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/config/SecurityConfig.java` - runtime JWKS retrieval supports key rotation at the IdP.
	- Recommendation: document crypto-agility requirements and provide configuration hooks for algorithm allowlists where needed.

#### V11.2.3 - Verify that all cryptographic primitives utilize a minimum of 128-bits of security based on the algorithm, key size, and configuration. For example, a 256-bit ECC key provides roughly 128 bits of security where RSA requires a 3072-bit key to achieve 128 bits of security.

- Status: `Partial`
- Implementation: token verification relies on IdP-provided keys; the application does not generate its own RSA/ECC key pairs. The effective security level depends on IdP key choices and JVM crypto provider configuration. No app-level enforcement of minimum key sizes is present.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/config/SecurityConfig.java` - uses IdP JWKS; recommend documenting minimum acceptable key sizes and enforcing via validators if required.

### V11.4 - Hashing and Hash-based Functions

#### V11.4.1 - Verify that only approved hash functions are used for general cryptographic use cases, including digital signatures, HMAC, KDF, and random bit generation. Disallowed hash functions, such as MD5, must not be used for any cryptographic purpose.

- Status: `Compliant`
- Implementation: the application does not implement custom hashing or signature algorithms; JWT signature verification uses algorithms published by the IdP in JWKS (typically RS256) and JVM providers for TLS/crypto. No MD5 usage found in the codebase.
- Evidence / Tests:
	- Codebase search: no usage of `MD5` or deprecated hash APIs detected.
	- `App/src/main/java/com/example/desofs/config/SecurityConfig.java` - delegates signature validation to Nimbus/JVM.

#### V11.4.2 - Verify that passwords are stored using an approved, computationally intensive, key derivation function (also known as a "password hashing function"), with parameter settings configured based on current guidance. The settings should balance security and performance to make brute-force attacks sufficiently challenging for the required level of security.

- Status: `Not Applicable`
- Implementation: the application does not store user passwords; authentication and password management are delegated to Auth0.
- Evidence / Tests:
	- `App/README.md` and `Auth0Config.java` - identity provider delegation.

#### V11.4.3 - Verify that hash functions used in digital signatures, as part of data authentication or data integrity are collision resistant and have appropriate bit-lengths. If collision resistance is required, the output length must be at least 256 bits. If only resistance to second pre-image attacks is required, the output length must be at least 128 bits.

- Status: `Partial`
- Implementation: digital signature hash choices are determined by the IdP's JWKS keys/algorithms; the application relies on IdP/JVM defaults. There is no app-level enforcement of hash selection; recommend documenting acceptable algorithms and enforcing via validators.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/config/SecurityConfig.java` - JWT decoder pipeline; recommend adding algorithm/algorithm-parameter checks.

### V11.5 - Random Values

#### V11.5.1 - Verify that all random numbers and strings which are intended to be non-guessable must be generated using a cryptographically secure pseudo-random number generator (CSPRNG) and have at least 128 bits of entropy. Note that UUIDs do not respect this condition.

- Status: `Compliant`
- Implementation: the application does not generate authentication secrets itself; where random values are used (tests or internal identifiers), standard JVM CSPRNGs should be used by libraries. No custom weak RNG usage detected in codebase.
- Evidence / Tests:
	- Codebase search: no usage of insecure RNGs found; no explicit `new Random()` for security-sensitive values.
	- Recommendation: document RNG requirements for any future crypto code.

### V11.6 - Public Key Cryptography

#### V11.6.1 - Verify that only approved cryptographic algorithms and modes of operation are used for key generation and seeding, and digital signature generation and verification. Key generation algorithms must not generate insecure keys vulnerable to known attacks, for example, RSA keys which are vulnerable to Fermat factorization.

- Status: `Partial`
- Implementation: key generation and signing are IdP responsibilities; the application delegates verification to Nimbus/JVM. There is no app-level key-generation code to audit. Recommend specifying acceptable algorithms and parameters in deployment docs.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/config/SecurityConfig.java` and `Auth0Config.java` - show reliance on IdP/JWKS for crypto.

#### V11.6.2 - Verify that approved cryptographic algorithms are used for key exchange (such as Diffie-Hellman) with a focus on ensuring that key exchange mechanisms use secure parameters. This will prevent attacks on the key establishment process which could lead to adversary-in-the-middle attacks or cryptographic breaks.

- Status: `Compliant`
- Implementation: key exchange for transport is handled by TLS (server/container and client); the application does not implement custom key exchange protocols. TLS parameters and server certs are deployment concerns.
- Evidence / Tests:
	- Deployment/config: TLS and container configuration expected to enforce secure key-exchange algorithms; no app-level key-exchange code.

### V11.7 - In-Use Data Cryptography

#### V11.7.2 - Verify that data minimization ensures the minimal amount of data is exposed during processing, and ensure that data is encrypted immediately after use or as soon as feasible.

- Status: `Partial`
- Implementation: the application minimizes sensitive data persistence (e.g., receipts stored in file storage with sanitization) and avoids storing passwords; encryption-at-rest for storage (database, file store) is expected to be provided by infrastructure. The repository does not contain application-level transparent encryption for persisted data.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/services/ReceiptFileService.java` - file sanitization and safe path construction for receipts.
	- `App/target/site/jacoco/jacoco.xml` and tests covering file handling.
	- Recommendation: document encryption-at-rest requirements and integrate with infrastructure secrets/crypto services.

## V12 - Secure Communication

### V12.1 - General TLS Security Guidance

#### V12.1.1 - Verify that only the latest recommended versions of the TLS protocol are enabled, such as TLS 1.2 and TLS 1.3. The latest version of the TLS protocol must be the preferred option.

- Status: `Partial`
- Implementation: TLS version negotiation and protocol configuration is enforced at the infrastructure/container layer (reverse proxy, load balancer, Tomcat). The application itself does not configure TLS protocol versions. `SecurityHeadersFilter` enforces HSTS which encourages HTTPS usage but does not control TLS versions.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/security/SecurityHeadersFilter.java` - HSTS header.
	- `App/src/test/java/com/example/desofs/config/TomcatConfigTest.txt` - container-related configuration tests and environment notes.
	- Recommendation: enforce TLS 1.2+/TLS 1.3 at the proxy/container and document this requirement in deployment docs.

#### V12.1.2 - Verify that only recommended cipher suites are enabled, with the strongest cipher suites set as preferred. L3 applications must only support cipher suites which provide forward secrecy.

- Status: `Partial`
- Implementation: cipher-suite selection is a deployment/infrastructure responsibility (reverse proxy/TLS terminator/JVM). The application does not override JVM cipher preferences. For L3-level assurance, configure the server/proxy to prefer forward-secure ciphers and document acceptable cipher lists.
- Evidence / Tests:
	- No app-level cipher configuration found; rely on container/proxy configuration and CVE/hardening guidance.
	- Recommendation: add deployment checklist with approved cipher suites and test vectors.

### V12.2 - HTTPS Communication with External Facing Services

#### V12.2.1 - Verify that TLS is used for all connectivity between a client and external facing, HTTP-based services, and does not fall back to insecure or unencrypted communications.

- Status: `Compliant`
- Implementation: the application expects and uses HTTPS for external-facing services (notably the IdP/JWKS endpoints). `Auth0Config` holds issuer and JWKS URIs which are HTTPS; token validation uses `JwtDecoders.fromIssuerLocation` which performs JWKS retrieval over HTTPS.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/config/Auth0Config.java` - issuer/JWKS configuration.
	- `App/src/main/java/com/example/desofs/config/SecurityConfig.java` - `jwtDecoder()` uses `JwtDecoders.fromIssuerLocation(issuerUri)` for JWKS retrieval (HTTPS).
	- `App/src/test/java/com/example/desofs/config/SecurityConfigIntegrationTest.txt` - integration evidence for auth configuration in tests.

#### V12.2.2 - Verify that external facing services use publicly trusted TLS certificates.

- Status: `Partial`
- Implementation: certificate trust for external services is enforced by the JVM/truststore and by the external infrastructure. The repository does not contain self-signed certificates or custom trust stores.
- Evidence / Tests:
	- No certificates or custom truststore configurations found in the repo; default JVM truststore is assumed.
	- Recommendation: document expectations (publicly trusted CAs) and, if required, include a CI smoke-test that validates the remote TLS certificate chain for critical endpoints (e.g., JWKS URI).

### V12.3 - General Service to Service Communication Security

#### V12.3.1 - Verify that an encrypted protocol such as TLS is used for all inbound and outbound connections to and from the application, including monitoring systems, management tools, remote access and SSH, middleware, databases, mainframes, partner systems, or external APIs. The server must not fall back to insecure or unencrypted protocols.

- Status: `Partial`
- Implementation: application-level code does not manage transport encryption for inbound connections (handled by container/infrastructure) and relies on HTTPS/JWKS for outbound token verification. Database connectivity and other infrastructure links are expected to use TLS, configured outside the repo (e.g., datasource URL with `jdbc:...ssl=true` or proxy settings).
- Evidence / Tests:
	- `App/src/main/resources/application.properties` and `App/target/classes/application-dev.properties` - no embedded plaintext certs; connection strings and TLS flags are expected to be provided via environment.
	- Recommendation: add explicit deployment checks to ensure DB and monitoring endpoints require TLS and do not accept plaintext connections.

#### V12.3.2 - Verify that TLS clients validate certificates received before communicating with a TLS server.

- Status: `Compliant`
- Implementation: outbound TLS client validation is performed by the JVM truststore by default. `JwtDecoders.fromIssuerLocation` and standard Spring HTTP clients (if used) rely on the JVM for certificate validation. There are no custom trust-all or insecure SSL contexts found in the codebase.
- Evidence / Tests:
	- Codebase search: no creation of insecure SSL contexts or `HostnameVerifier` overrides detected.
	- `App/src/main/java/com/example/desofs/config/SecurityConfig.java` - usage of standard `JwtDecoder` facilities which use HTTPS and JVM validation.
	- Recommendation: add a CI test that fails on HTTP endpoints or invalid certificate chains for critical external services.

#### V12.3.3 - Verify that TLS or another appropriate transport encryption mechanism used for all connectivity between internal, HTTP-based services within the application, and does not fall back to insecure or unencrypted communications.

- Status: `Partial`
- Implementation: internal service-to-service encryption is considered an infrastructure responsibility. The application does not enforce mutual-TLS or DPoP/PoP for service-to-service calls; it relies on network segmentation and deployment-level TLS. For higher assurance, document and enable mTLS or token-based service authentication in the deployment topology.
- Evidence / Tests:
	- No in-repo mTLS configuration or client certificate handling found.
	- Recommendation: define service-authentication requirements in deployment docs and consider adding mTLS or service identity tokens for internal API calls.

## V13 - Configuration

### V13.1 - Configuration Documentation

#### V13.1.1 - Verify that all communication needs for the application are documented. This must include external services which the application relies upon and cases where an end user might be able to provide an external location to which the application will then connect.

- Status: `Partial`
- Implementation: external dependencies are documented in `README.md`, `Deliverables/Phase2` docs and in-code configuration classes such as `Auth0Config`. The repository contains examples of the external services used (Auth0, DB) but lacks a single consolidated communication matrix listing endpoints, protocols and allowed origin/egress hosts.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/config/Auth0Config.java` - issuer/JWKS endpoints.
	- `Deliverables/Phase1/README.md` and project `README.md` - high-level architecture notes referencing external services.
	- Recommendation: add a `communication-matrix.md` listing all external hosts, ports, protocols, and whether the application may follow user-provided URLs.

#### V13.1.2 - Verify that for each service the application uses, the documentation defines the maximum number of concurrent connections (e.g., connection pool limits) and how the application behaves when that limit is reached, including any fallback or recovery mechanisms, to prevent denial of service conditions.

- Status: `Partial`
- Implementation: connection-pool behaviour is determined by library defaults (e.g., Hikari) and environment-provided properties; there is no single documentation artifact enumerating pool sizes and fallback behaviour. The codebase relies on Spring Boot / Hikari defaults unless overridden via environment configuration.
- Evidence / Tests:
	- `App/target/classes/application.properties` contains connection-related sections; Hikari classes appear in runtime dependencies (evidence in JaCoCo output). No explicit in-repo documentation of connection limits.
	- Recommendation: document configured pool sizes (Hikari `maximumPoolSize`, datasource timeouts) and add CI smoke-tests for connection exhaustion scenarios if required.

#### V13.1.3 - Verify that the application documentation defines resource‑management strategies for every external system or service it uses (e.g., databases, file handles, threads, HTTP connections). This should include resource‑release procedures, timeout settings, failure handling, and where retry logic is implemented, specifying retry limits, delays, and back‑off algorithms. For synchronous HTTP request–response operations it should mandate short timeouts and either disable retries or strictly limit retries to prevent cascading delays and resource exhaustion.

- Status: `Partial`
- Implementation: some resource-management concerns are implemented in code (e.g., sensible timeouts expected via RestTemplate/HTTP client and DB pooling) but a consolidated strategy document is not present. Retry behaviour is not implemented centrally in application code; timeouts should be provided via environment or HTTP client configuration in deployments.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/services/ReceiptFileService.java` - file resource handling shows explicit close/safe usage patterns in tests.
	- Recommendation: add a resource-management section listing timeouts, retry policies, and threadpool sizing for critical subsystems.

#### V13.1.4 - Verify that the application's documentation defines the secrets that are critical for the security of the application and a schedule for rotating them, based on the organization's threat model and business requirements.

- Status: `Partial`
- Implementation: secrets (database credentials, Auth0 client/issuer settings) are loaded via properties/environment; the repository avoids storing secrets in source control. There is no documented rotation schedule or secrets inventory in the repo.
- Evidence / Tests:
	- `App/target/classes/application-dev.properties` and `App/src/test/resources/application-test.properties` - show property-based configuration for tests and dev without committed secrets.
	- Recommendation: adopt a secrets management policy and a rotation schedule; integrate with a vault (Azure Key Vault, AWS Secrets Manager, HashiCorp Vault) for L2/L3 environments.

### V13.2 - Backend Communication Configuration

#### V13.2.1 - Verify that communications between backend application components that don't support the application's standard user session mechanism, including APIs, middleware, and data layers, are authenticated. Authentication must use individual service accounts, short-term tokens, or certificate-based authentication and not unchanging credentials such as passwords, API keys, or shared accounts with privileged access.

- Status: `Partial`
- Implementation: the application authenticates to external services where required via credentials provided in configuration (datasource credentials) and delegates authentication for user flows to Auth0. There is no in-repo use of short-lived service tokens or mTLS for backend-to-backend authentication; these are expected to be provided by infrastructure/service mesh if required.
- Evidence / Tests:
	- `App/src/main/resources/application.properties` and `Auth0Config.java` - show where credentials and endpoints are configured.
	- Recommendation: migrate sensitive service connections to short-lived credentials or mTLS and document service account usage.

#### V13.2.2 - Verify that communications between backend application components, including local or operating system services, APIs, middleware, and data layers, are performed with accounts assigned the least necessary privileges.

- Status: `Partial`
- Implementation: least privilege for database accounts and external integrations should be enforced at deployment; application code uses repository/DAO patterns and does not assume elevated privileges. There is no in-code enforcement of least-privilege beyond separation of concerns.
- Evidence / Tests:
	- No explicit `root` or default credentials in the codebase; review deployment docs to ensure per-service least-privilege accounts are used.
	- Recommendation: document required privileges per service account and include checks during deployment.

#### V13.2.3 - Verify that if a credential has to be used for service authentication, the credential being used by the consumer is not a default credential (e.g., root/root or admin/admin).

- Status: `Compliant`
- Implementation: no default credentials are committed in the repository. Examples and tests use in-memory or test credentials for local runs; production credentials are expected to be injected via environment or secrets store.
- Evidence / Tests:
	- Repo search: no occurrences of `root/root` or similar default credentials in source files.

#### V13.2.4 - Verify that an allowlist is used to define the external resources or systems with which the application is permitted to communicate (e.g., for outbound requests, data loads, or file access). This allowlist can be implemented at the application layer, web server, firewall, or a combination of different layers.

- Status: `Not Implemented`
- Implementation: no application-level outbound allowlist was found. Network egress policies and firewall-based allowlists are expected to be enforced by infrastructure. The codebase contains no explicit host allowlist for outbound HTTP calls.
- Evidence / Tests:
	- Recommendation: implement an outbound allowlist (application config or infra) for critical environments and include tests that fail on unexpected external hostnames.

#### V13.2.5 - Verify that the web or application server is configured with an allowlist of resources or systems to which the server can send requests or load data or files from.

- Status: `Partial`
- Implementation: server/proxy allowlisting is a deployment concern; the application does not embed server-level allowlists. Container/configuration tests (TomcatConfig) demonstrate container configuration is considered.
- Evidence / Tests:
	- `App/src/test/java/com/example/desofs/config/TomcatConfigTest.txt` - container-related tests and documentation.
	- Recommendation: enforce allowlist at server/proxy and document it in deployment manifests.

#### V13.2.6 - Verify that where the application connects to separate services, it follows the documented configuration for each connection, such as maximum parallel connections, behavior when maximum allowed connections is reached, connection timeouts, and retry strategies.

- Status: `Partial`
- Implementation: connection behaviour is configurable via environment properties; the repo lacks a central source of truth pairing connections to their runtime parameters. Hikari/JDBC and HTTP clients can be configured, but values are not consolidated.
- Evidence / Tests:
	- `App/target/classes/application.properties` and dependency evidence for Hikari in JaCoCo output.
	- Recommendation: consolidate connection configuration and document behaviours per external service.

### V13.3 - Secret Management

#### V13.3.1 - Verify that a secrets management solution, such as a key vault, is used to securely create, store, control access to, and destroy backend secrets. These could include passwords, key material, integrations with databases and third-party systems, keys and seeds for time-based tokens, other internal secrets, and API keys. Secrets must not be included in application source code or included in build artifacts. For an L3 application, this must involve a hardware-backed solution such as an HSM.

- Status: `Partial`
- Implementation: the repository does not store secrets in source control and relies on environment-based configuration. There is no integration with a centralized secrets manager (e.g., Vault) in application code.
- Evidence / Tests:
	- `App/src/test/resources` and `App/target/classes/application-dev.properties` - show property-based configuration for tests and dev without committed secrets.
	- Recommendation: integrate with a secrets manager for production deployments and, for L3 targets, consider HSM-backed key storage.

#### V13.3.2 - Verify that access to secret assets adheres to the principle of least privilege.

- Status: `Partial`
- Implementation: access controls for secrets are delegated to deployment environment; application code reads secrets but does not manage access policies. Recommend IAM policies around secret access.
- Evidence / Tests:
	- No in-repo secrets access controls; recommend documenting required IAM roles and least-privilege access rules in deployment docs.

#### V13.3.3 - Verify that all cryptographic operations are performed using an isolated security module (such as a vault or hardware security module) to securely manage and protect key material from exposure outside of the security module.

- Status: `Not Implemented`
- Implementation: cryptographic operations are delegated to external IdP (Auth0) and JVM crypto providers; there is no use of an in-repo isolated security module or HSM.
- Evidence / Tests:
	- Recommendation: if required by threat model, integrate an HSM or vault-backed crypto provider and update tests accordingly.

#### V13.3.4 - Verify that secrets are configured to expire and be rotated based on the application's documentation.

- Status: `Not Implemented`
- Implementation: no rotation schedules or expiration policies are documented in the repo. Rotation is expected to be handled by the secret-management system or IdP in deployments.
- Evidence / Tests:
	- Recommendation: publish a secrets-rotation policy and automate rotation verification where possible.

### V13.4 - Unintended Information Leakage

#### V13.4.1 - Verify that the application is deployed either without any source control metadata, including the .git or .svn folders, or in a way that these folders are inaccessible both externally and to the application itself.

- Status: `Partial`
- Implementation: repository contains VCS metadata locally (as a normal development repo) but deployment artifacts and packaged JARs in `target/` do not include `.git` folders. Deployment packaging should exclude VCS metadata.
- Evidence / Tests:
	- `target/` artifacts (JAR) and CI packaging steps avoid including repository metadata.
	- Recommendation: add CI verification to fail builds that accidentally include `.git` or other VCS metadata in artifacts.

#### V13.4.2 - Verify that debug modes are disabled for all components in production environments to prevent exposure of debugging features and information leakage.

- Status: `Compliant`
- Implementation: `server.error.include-stacktrace=never` in application properties and unit/integration tests assert that error responses do not expose stack traces. Production profiles should not enable debug logging.
- Evidence / Tests:
	- `App/target/classes/application.properties` - `server.error.include-stacktrace=never`.
	- `App/src/test/java/com/example/desofs/config/SecurityConfigIntegrationTest.java` - test `errorResponse_noStackTrace` ensures stack traces are not exposed.

#### V13.4.3 - Verify that web servers do not expose directory listings to clients unless explicitly intended.

- Status: `Compliant`
- Implementation: the application serves static content via Spring Boot defaults and no directory-listing endpoints are implemented. Web server/container should be configured to disallow directory listings for deployed static assets.
- Evidence / Tests:
	- Codebase review: no controllers or static resource configs that intentionally expose directory listings.
	- Recommendation: confirm container static resource settings during deployment.

#### V13.4.4 - Verify that using the HTTP TRACE method is not supported in production environments, to avoid potential information leakage.

- Status: `Partial`
- Implementation: TRACE/DEBUG methods are typically disabled by default at the server/container; the application does not enable TRACE. Recommend verifying web server configuration to explicitly disable TRACE.
- Evidence / Tests:
	- No server-side handlers for TRACE found in the codebase; recommend a deployment-level check.

#### V13.4.5 - Verify that documentation (such as for internal APIs) and monitoring endpoints are not exposed unless explicitly intended.

- Status: `Partial`
- Implementation: Spring Boot Actuator is included but application properties limit exposed endpoints to `health` by default (`management.endpoints.web.exposure.include=health` in packaged `application.properties`). Documentation and internal endpoints should be restricted in production via environment configuration.
- Evidence / Tests:
	- `App/target/classes/application.properties` - actuator exposure config.
	- `pom.xml` includes `spring-boot-starter-actuator` artifact.
	- Recommendation: ensure actuator and any documentation endpoints are accessible only from trusted networks or protected via auth.

#### V13.4.6 - Verify that the application does not expose detailed version information of backend components.

- Status: `Partial`
- Implementation: application does not intentionally expose detailed component versions via API; some build artifacts contain version metadata in `pom.properties` (packaging-time) which should not be exposed publicly. Ensure management endpoints do not disclose versioning to unauthenticated clients.
- Evidence / Tests:
	- `target/maven-archiver/pom.properties` contains packaged artifact metadata; tests assert error responses hide debug info.
	- Recommendation: remove or restrict any endpoints that reveal version details.

#### V13.4.7 - Verify that the web tier is configured to only serve files with specific file extensions to prevent unintentional information, configuration, and source code leakage.

- Status: `Partial`
- Implementation: static resource serving uses Spring Boot defaults; the application sanitizes and controls file writes in `ReceiptFileService`. There is no explicit extension-allowlist for served files in code.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/services/ReceiptFileService.java` and related tests that assert sanitization and safe file handling.
	- Recommendation: enforce an allowlist of served file extensions in static resource configuration or via a filter.

## V14 - Data Protection


### V14.1 - Data Protection Documentation

#### V14.1.1 - Verify that all sensitive data created and processed by the application has been identified and classified into protection levels. This includes data that is only encoded and therefore easily decoded, such as Base64 strings or the plaintext payload inside a JWT. Protection levels need to take into account any data protection and privacy regulations and standards which the application is required to comply with.

- Status: `Partial`
- Implementation: the project contains high-level descriptions of data flows and examples in `README.md` and deliverables, but there is no single, authoritative data classification inventory. The codebase demonstrates limited storage of identifying values (Auth0 `sub`) and application artifacts (receipts), which should be classified as at least "sensitive" for retention and access controls.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/controllers/OrderController.java` - uses `jwt.getSubject()` for user mapping.
	- `App/src/main/java/com/example/desofs/services/ReceiptFileService.java` - receipt creation and storage examples.
	- Recommendation: create `data-inventory.md` listing types, classification levels, retention requirements and allowed processing.

#### V14.1.2 - Verify that all sensitive data protection levels have a documented set of protection requirements. This must include (but not be limited to) requirements related to general encryption, integrity verification, retention, how the data is to be logged, access controls around sensitive data in logs, database-level encryption, privacy and privacy-enhancing technologies to be used, and other confidentiality requirements.

- Status: `Partial`
- Implementation: protection requirements are partially documented in architecture notes and recommendations in Traceability, but there is no consolidated protection-requirements artefact mapping each classified datum to controls.
- Evidence / Tests:
	- `Deliverables/Phase1/README.md` and `Deliverables/Phase2` docs - high-level guidance.
	- Recommendation: author `data-protection-requirements.md` that maps protection controls to classified data.

### V14.2 - General Data Protection

#### V14.2.1 - Verify that sensitive data is only sent to the server in the HTTP message body or header fields, and that the URL and query string do not contain sensitive information, such as an API key or session token.

- Status: `Compliant`
- Implementation: controllers accept JSON bodies and headers (e.g., Authorization Bearer tokens) and the code does not place sensitive tokens or PII in query strings. Tests exercise input validation and ensure sensitive fields are passed in bodies or headers.
- Evidence / Tests:
	- `App/src/test/java/com/example/desofs/controller/InputValidationSecurityTest.java` - tests for input handling and sanitization.

#### V14.2.2 - Verify that the application prevents sensitive data from being cached in server components, such as load balancers and application caches, or ensures that the data is securely purged after use.

- Status: `Partial`
- Implementation: `SecurityHeadersFilter` provides cache-control headers for responses where appropriate, but application-level cache usage and purge policies are not comprehensively documented. Infrastructure caches and proxies should be configured to respect cache headers for sensitive endpoints.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/security/SecurityHeadersFilter.java` - header settings (CSP, HSTS, etc.).
	- Recommendation: verify server/proxy caching policies and add tests for `Cache-Control` where sensitive payloads are returned.

#### V14.2.3 - Verify that defined sensitive data is not sent to untrusted parties (e.g., user trackers) to prevent unwanted collection of data outside of the application's control.

- Status: `Compliant`
- Implementation: the backend does not include third‑party trackers; outbound calls are limited to known services (IdP, DB). No telemetry code that posts PII to third-party trackers found in the codebase.
- Evidence / Tests:
	- Codebase review: no tracking/analytics libraries identified in `pom.xml` or code.

#### V14.2.4 - Verify that controls around sensitive data related to encryption, integrity verification, retention, how the data is to be logged, access controls around sensitive data in logs, privacy and privacy-enhancing technologies, are implemented as defined in the documentation for the specific data's protection level.

- Status: `Partial`
- Implementation: some controls are implemented (no stack traces in error responses, sanitization on file names), while others (logging redaction rules, retention enforcement, encryption at rest) are expected to be handled by infrastructure or documented separately. The codebase provides sanitization tests and security header tests but lacks centralized logging-redaction configuration.
- Evidence / Tests:
	- `App/src/test/java/com/example/desofs/services/ReceiptFileServiceTest.java` - sanitization tests.
	- `App/src/test/java/com/example/desofs/config/SecurityConfigIntegrationTest.java` - error response tests.
	- Recommendation: publish mapping from classified data to the protective controls and implement CI checks for logging and retention rules.

## V15 - Secure Coding and Archite

### V15.1 - Secure Coding and Architecture Documentation

#### V15.1.1 - Verify that application documentation defines risk based remediation time frames for 3rd party component versions with vulnerabilities and for updating libraries in general, to minimize the risk from these components.

- Status: `Partial`
- Implementation: dependency versions are declared in `pom.xml` and the project uses Maven for builds, but there is no documented remediation timeline or policy in the repository. Vulnerability response is expected to be handled by maintainers; recommend formalising SLA (e.g., critical within 48h, high within 7 days).
- Evidence / Tests:
	- `pom.xml` - dependency declarations.
	- `App/target/surefire-reports/` and `App/target/site/jacoco/` - test and coverage artefacts demonstrating test automation exists.
	- Recommendation: publish a dependency remediation policy and integrate automated alerts (Dependabot/GitHub Security) into the workflow.

#### V15.1.2 - Verify that an inventory catalog, such as software bill of materials (SBOM), is maintained of all third-party libraries in use, including verifying that components come from pre-defined, trusted, and continually maintained repositories.

- Status: `Not Implemented`
- Implementation: no SBOM or automated SBOM generation is present in the repository. The `pom.xml` implicitly lists dependencies but an SBOM (CycloneDX/SPDX) should be generated and stored for releases.
- Evidence / Tests:
	- `pom.xml` shows all dependencies; recommendation: add a Maven plugin to generate SBOMs and store them alongside releases.

#### V15.1.3 - Verify that the application documentation identifies functionality which is time-consuming or resource-demanding. This must include how to prevent a loss of availability due to overusing this functionality and how to avoid a situation where building a response takes longer than the consumer's timeout. Potential defenses may include asynchronous processing, using queues, and limiting parallel processes per user and per application.

- Status: `Partial`
- Implementation: time-consuming operations are considered in design notes and code (e.g., no heavy synchronous work in controllers). `RateLimitFilter` mitigates abusive request rates. However, there is no centralized documentation mapping expensive operations to mitigation strategies (queues, async processing) for each identified endpoint.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/security/RateLimitFilter.java` - rate-limit implementation.
	- Recommendation: catalogue expensive operations and document mitigation strategies (circuit breakers, async processing, queueing) and add tests for degraded behaviour.

### V15.2 - Security Architecture and Dependencies

#### V15.2.1 - Verify that the application only contains components which have not breached the documented update and remediation time frames.

- Status: `Partial`
- Implementation: the project keeps dependencies in `pom.xml` but lacks automated enforcement of remediation time frames. CI includes unit/integration tests, but SCA enforcement should be added to prevent known-vulnerable components from being merged.
- Evidence / Tests:
	- `pom.xml` - dependency list.
	- Recommendation: add SCA checks in CI (OWASP Dependency-Check, GitHub Dependabot) and block merges for critical/high vulnerabilities until remediated.

#### V15.2.2 - Verify that the application has implemented defenses against loss of availability due to functionality which is time-consuming or resource-demanding, based on the documented security decisions and strategies for this.

- Status: `Partial`
- Implementation: `RateLimitFilter` exists and tests cover rate-limiting behaviour. No circuit-breaker or bulkhead patterns were found in application code; these are recommended for external calls.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/security/RateLimitFilter.java` and tests.
	- Recommendation: add circuit-breaker/bulkhead for outbound calls (Resilience4j), and document behaviour under resource exhaustion.

#### V15.2.3 - Verify that the production environment only includes functionality that is required for the application to function, and does not expose extraneous functionality such as test code, sample snippets, and development functionality.

- Status: `Compliant`
- Implementation: build artifacts in `target/` exclude test sources, and production packaging should not include development-only resources. Developers must ensure CI packaging uses production profile and strips dev/test artefacts.
- Evidence / Tests:
	- `target/` packaging and standard Maven build outputs; no dev-only resources observed in production packaging.
	- Recommendation: add CI checks to validate production packaging profile and to ensure no test/debug artifacts are included in releases.

### V15.3 - Defensive Coding

#### V15.3.1 - Verify that the application only returns the required subset of fields from a data object. For example, it should not return an entire data object, as some individual fields should not be accessible to users.

- Status: `Compliant`
- Implementation: controllers and DTOs restrict fields returned to clients; DTO mapping and repository queries avoid returning internal-only fields. Controllers use explicit DTOs rather than exposing entity objects directly.
- Evidence / Tests:
	- Controller DTO usage in `App/src/main/java/com/example/desofs/shared/dtos/` and controller test coverage (`MovieControllerIntegrationTests`, `OrderControllerIntegrationTests`).

#### V15.3.2 - Verify that where the application backend makes calls to external URLs, it is configured to not follow redirects unless it is intended functionality.

- Status: `Partial`
- Implementation: outbound HTTP clients are not broadly used in application code; where used, default client behaviour applies. There is no global configuration forcing non-following of redirects; recommend explicitly configuring HTTP clients (RestTemplate/WebClient) to disable or validate redirects when calling untrusted endpoints.
- Evidence / Tests:
	- Codebase search found references to Spring HTTP client classes in dependencies; add explicit client configuration and tests if redirect behaviour is relevant to a flow.

#### V15.3.3 - Verify that the application has countermeasures to protect against mass assignment attacks by limiting allowed fields per controller and action, e.g., it is not possible to insert or update a field value when it was not intended to be part of that action.

- Status: `Compliant`
- Implementation: DTOs and `@JsonProperty` mapping define explicit fields accepted from clients; service layer enforces permitted updates rather than accepting arbitrary mapped entities.
- Evidence / Tests:
	- DTO classes and validation annotations under `App/src/main/java/com/example/desofs/shared/dtos/` and unit/integration tests verifying update behaviours.

#### V15.3.4 - Verify that all proxying and middleware components transfer the user's original IP address correctly using trusted data fields that cannot be manipulated by the end user, and the application and web server use this correct value for logging and security decisions such as rate limiting, taking into account that even the original IP address may not be reliable due to dynamic IPs, VPNs, or corporate firewalls.

- Status: `Partial`
- Implementation: the application relies on container/proxy headers (`ForwardedHeaderFilter`/proxy config) for original IP; `RateLimitFilter` should be configured to use the correct source IP from trusted headers. Ensure infrastructure sets trusted proxies and the app uses Spring's forwarded header handling.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/security/RateLimitFilter.java` - rate-limiting filter; recommendation: document and test forwarded-header trust boundaries.

#### V15.3.5 - Verify that the application explicitly ensures that variables are of the correct type and performs strict equality and comparator operations. This is to avoid type juggling or type confusion vulnerabilities caused by the application code making an assumption about a variable type.

- Status: `Compliant`
- Implementation: Java's static typing enforces types at compile-time; input validation and DTO binding ensure runtime values conform to expected types. Where parsing from strings occurs, explicit parsing and validation are used.
- Evidence / Tests:
	- DTO validation annotations and unit tests covering invalid-type inputs (`InputValidationSecurityTest`).

#### V15.3.7 - Verify that the application has defenses against HTTP parameter pollution attacks, particularly if the application framework makes no distinction about the source of request parameters (query string, body parameters, cookies, or header fields).

- Status: `Partial`
- Implementation: Spring MVC distinguishes parameter sources by mapping; controllers use typed method parameters and DTOs. For added safety, validate multi-value parameters and canonicalize inputs. Recommend adding tests for parameter-pollution edge cases on endpoints that parse query parameters into collections.
- Evidence / Tests:
	- Controller signatures and DTO mapping; `InputValidationSecurityTest` covers many input edge cases but not exhaustive HTTP parameter pollution cases.


### V15.4 - Safe Concurrency

#### V15.4.1 - Verify that shared objects in multi-threaded code (such as caches, files, or in-memory objects accessed by multiple threads) are accessed safely by using thread-safe types and synchronization mechanisms like locks or semaphores to avoid race conditions and data corruption.

- Status: `Partial`
- Implementation: the codebase is primarily request-scoped; shared resources (e.g., caches, Hikari pool) are provided by libraries. Custom shared data structures are minimal; where present ensure thread-safety. Recommend code review for any singleton mutable state.
- Evidence / Tests:
	- Use of standard thread-safe libraries (Hikari, Spring-managed beans). No explicit multi-threaded shared state found in application code; recommend scanning for singletons with mutable fields.

#### V15.4.2 - Verify that checks on a resource's state, such as its existence or permissions, and the actions that depend on them are performed as a single atomic operation to prevent time-of-check to time-of-use (TOCTOU) race conditions. For example, checking if a file exists before opening it, or verifying a user’s access before granting it.

- Status: `Partial`
- Implementation: file operations in `ReceiptFileService` perform safe path construction and use atomic write patterns where possible; however a full TOCTOU audit is not present. Recommend auditing critical resources and adding integration tests that simulate race conditions if applicable.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/services/ReceiptFileService.java` - safe file handling and tests.

#### V15.4.3 - Verify that locks are used consistently to avoid threads getting stuck, whether by waiting on each other or retrying endlessly, and that locking logic stays within the code responsible for managing the resource to ensure locks cannot be inadvertently or maliciously modified by external classes or code.

- Status: `Compliant` (no custom locking)
- Implementation: the application does not implement custom coarse-grained locking; concurrency is primarily handled by framework and libraries. When custom locks are required, they should be localised and documented.
- Evidence / Tests:
	- No custom global lock implementations found in the codebase.

#### V15.4.4 - Verify that resource allocation policies prevent thread starvation by ensuring fair access to resources, such as by leveraging thread pools, allowing lower-priority threads to proceed within a reasonable timeframe.

- Status: `Partial`
- Implementation: thread pools are managed by the container and Spring; tuning of thread pool sizes and fair scheduling is a deployment concern. Recommend documenting thread pool sizing for production and adding load tests to detect starvation scenarios.
- Evidence / Tests:
	- Use of Spring container-managed thread pools and Hikari connection pool; no custom thread-pool configurations in the repo.

## V16 - Security Logging and Erro

### V16.1 - Security Logging Documentation

#### V16.1.1 - Verify that an inventory exists documenting the logging performed at each layer of the application's technology stack, what events are being logged, log formats, where that logging is stored, how it is used, how access to it is controlled, and for how long logs are kept.

- Status: `Partial`
- Implementation: application-level logging is present via standard frameworks (SLF4J/Logback via Spring Boot) and audit endpoints exist (AuditLogController). However, there is no repository-stored log inventory or centralised documentation mapping each logged event to retention/access controls in the repo.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/controllers/AuditLogController.java`, `App/src/main/java/com/example/desofs/services/AuditLogService.java`, and related tests in `App/target/surefire-reports/`.
	- `Deliverables/Phase2/Sprint1/Development/development.md` documents the implemented audit events.
	- Recommendation: produce and store a standalone log inventory document (CSV/Markdown) listing events, retention, sensitivity, and access roles.

### V16.2 - General Logging

#### V16.2.1 - Verify that each log entry includes necessary metadata (such as when, where, who, what) that would allow for a detailed investigation of the timeline when an event happens.

- Status: `Compliant`
- Implementation: audit log entries include `actorId`, `targetUserId`, `role`, `operation`, and `timestamp`, which together provide the required who/what/when context for audited business events. Standard SLF4J request logs remain in place for operational tracing.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/domain/AuditLog.java`
	- `App/src/main/java/com/example/desofs/services/AuditLogService.java`
	- `App/src/test/java/com/example/desofs/services/AuditLogServiceTest.java`
	- `App/src/test/java/com/example/desofs/controller/AuditLogControllerIntegrationTests.java`

#### V16.2.2 - Verify that time sources for all logging components are synchronized, and that timestamps in security event metadata use UTC or include an explicit time zone offset. UTC is recommended to ensure consistency across distributed systems and to prevent confusion during daylight saving time transitions.

- Status: `Not Verified`
- Implementation: project relies on JVM/system time; no repository evidence of explicit timezone enforcement for logs. Recommend configuring the JVM and logging format to emit UTC timestamps.
- Evidence / Tests:
	- No explicit timezone configuration found in `application.properties`; recommend adding `spring.jackson.time-zone=UTC` and logging pattern including UTC.

#### V16.2.3 - Verify that the application only stores or broadcasts logs to the files and services that are documented in the log inventory.

- Status: `Partial`
- Implementation: application writes to local logs by default and can be configured to send logs to external systems via Logback appenders; inventory and enforcement are not present in-repo.
- Evidence / Tests:
	- Default Spring Boot logging configuration in `application.properties` and `target/` outputs; recommendation: document logging sinks and enforce with CI checks.

#### V16.2.4 - Verify that logs can be read and correlated by the log processor that is in use, preferably by using a common logging format.

- Status: `Partial`
- Implementation: structured logging not enforced; correlation IDs and request IDs are recommended to correlate logs across services.
- Evidence / Tests:
	- Recommendation: add correlation ID middleware and include it in logs via MDC.

#### V16.2.5 - Verify that when logging sensitive data, the application enforces logging based on the data's protection level. For example, it may not be allowed to log certain data, such as credentials or payment details. Other data, such as session tokens, may only be logged by being hashed or masked, either in full or partially.

- Status: `Partial`
- Implementation: tests and code indicate attention to sensitive data (no direct logging of tokens in tests), but there is no central log-redaction configuration. Recommend adding log sanitization utilities and CI checks that scan logs for secrets.
- Evidence / Tests:
	- `CustomErrorControllerTest`, `InputValidationSecurityTest` cover error responses; recommendation: implement redaction policy and automated checks.

### V16.3 - Security Events

#### V16.3.1 - Verify that all authentication operations are logged, including successful and unsuccessful attempts. Additional metadata, such as the type of authentication or factors used, should also be collected.

- Status: `Partial`
- Implementation: Spring Security integration logs some authentication events; for full coverage ensure successful and failed attempts are consistently logged and routed to the audit pipeline.
- Evidence / Tests:
	- Security-related tests in `App/target/surefire-reports/`; recommendation: add explicit authentication audit events and tests.

#### V16.3.2 - Verify that failed authorization attempts are logged. For L3, this must include logging all authorization decisions, including logging when sensitive data is accessed (without logging the sensitive data itself).

- Status: `Partial`
- Implementation: failed authorization events are logged by default in Spring Security; application-level logging for authorization decisions (esp. sensitive data access) should be added where necessary.
- Evidence / Tests:
	- `App/src/test/.../Auth*` tests and surefire reports; recommendation: add audit hooks to service-layer authorization checks.

#### V16.3.3 - Verify that the application logs the security events that are defined in the documentation and also logs attempts to bypass the security controls, such as input validation, business logic, and anti-automation.

- Status: `Partial`
- Implementation: the documented audit events are now implemented for role changes, movie creation, order creation, and refund lifecycle actions. Validation bypass attempts and anti-automation attempts are still handled through tests and generic error responses rather than dedicated audit events.
- Evidence / Tests:
	- Unit/integration tests and code references.
	- Recommendation: expand the event catalog if future phases require dedicated logging of validation-bypass or rate-limit abuse attempts.

#### V16.3.4 - Verify that the application logs unexpected errors and security control failures such as backend TLS failures.

- Status: `Compliant`
- Implementation: application logs unexpected errors via exception handlers and the `CustomErrorController` implementation; TLS/network failures will be surfaced by the runtime and should be captured by logging.
- Evidence / Tests:
	- `CustomErrorControllerTest` and sure-fire reports; recommendation: ensure operational logs (infrastructure) are ingested by the monitoring pipeline.

### V16.4 - Log Protection

#### V16.4.1 - Verify that all logging components appropriately encode data to prevent log injection.

- Status: `Partial`
- Implementation: application-level logging uses formatters; explicit encoding for logs is not centralised. Recommend implementing log-output encoding utilities and testing for log injection.
- Evidence / Tests:
	- Recommendation: add unit tests asserting dangerous characters are escaped when included in log messages.

#### V16.4.2 - Verify that logs are protected from unauthorized access and cannot be modified.

- Status: `Partial`
- Implementation: the audit trail is stored in a dedicated database table and is only exposed through an ADMIN-only controller endpoint. Full log immutability and storage hardening still depend on deployment and database policy.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/controllers/AuditLogController.java`
	- Recommendation: document log access controls and retention in the operations runbook and ensure transport uses TLS and logs are immutable when possible (WORM/append-only storage).

#### V16.4.3 - Verify that logs are securely transmitted to a logically separate system for analysis, detection, alerting, and escalation. The aim is to ensure that if the application is breached, the logs are not compromised.

- Status: `Not Implemented`
- Implementation: no in-repo configuration for log shipping to external systems. Recommend configuring Logback appenders or agent-based shipping (Filebeat) to a secured, separate logging cluster.
- Evidence / Tests:
	- Recommendation: document and automate deployment configuration for secure log shipping.

### V16.5 - Error Handling

#### V16.5.1 - Verify that a generic message is returned to the consumer when an unexpected or security-sensitive error occurs, ensuring no exposure of sensitive internal system data such as stack traces, queries, secret keys, and tokens.

- Status: `Compliant`
- Implementation: `CustomErrorController` and integration tests ensure generic error responses; stack traces are not returned in API responses in production profile.
- Evidence / Tests:
	- `App/src/test/java/com/example/desofs/controller/CustomErrorControllerTest.java` and integration tests.

#### V16.5.2 - Verify that the application continues to operate securely when external resource access fails, for example, by using patterns such as circuit breakers or graceful degradation.

- Status: `Partial`
- Implementation: graceful degradation patterns are not implemented across the codebase; recommend adding circuit-breakers for external calls and fallback strategies.
- Evidence / Tests:
	- Recommendation: add Resilience4j for external calls and tests for degraded modes.

#### V16.5.3 - Verify that the application fails gracefully and securely, including when an exception occurs, preventing fail-open conditions such as processing a transaction despite errors resulting from validation logic.

- Status: `Partial`
- Implementation: exception handlers are present; business-critical flows should be reviewed for fail-open logic and additional tests added to simulate partial failures.
- Evidence / Tests:
	- `CustomErrorControllerTest` and service-layer tests; recommendation: add chaos or fault-injection tests for critical flows.


#### V16.5.4 - Verify that a "last resort" error handler is defined which will catch all unhandled exceptions. This is both to avoid losing error details that must go to log files and to ensure that an error does not take down the entire application process, leading to a loss of availability.

- Status: `Partial`
- Implementation: the project includes `CustomErrorController` and global exception handling via Spring Boot mechanisms which provide generic API responses and avoid exposing stack traces to clients. However, there is no explicit global uncaught-thread exception handler configured in application code and no documented operational runbook describing process supervision and restart policies.
- Evidence / Tests:
	- `App/src/main/java/com/example/desofs/controller/CustomErrorController.java` and `App/src/test/java/com/example/desofs/controller/CustomErrorControllerTest.java`.
	- `App/target/classes/application.properties` - `server.error.include-stacktrace=never` confirms stack traces aren't returned to clients.
- Recommendation: ensure a JVM-level `Thread.setDefaultUncaughtExceptionHandler(...)` is configured for long-running/non-request threads and document process supervision (systemd/service manager, container restart policy). Also ensure the last-resort handler logs full stack traces at error level while returning generic messages to clients; add tests or chaos scenarios to validate availability under uncaught exceptions.
