# Use Case 2: Browse Available Movies

## 1. Description
### 1.1 Objective
This Use Case allows authenticated Customers to retrieve and view the list of available movies in the eMovieShop catalog.
It provides customers with visibility into what movies are currently on offer, including titles, genres, prices, and stock 
availability, so they can make informed purchasing decisions.

### 1.2 Actors
* **Customer:** Primary actor browsing the movie catalog.

### 1.3 Use/Abuse Case Diagram
This diagram illustrates the legitimate path for retrieving the movie catalog versus potential abuse scenarios, such as 
unauthenticated users attempting to access the catalog or attackers scraping pricing and availability data.

![Use and Abuse Cases - UC2](./diagrams/images/UseAbuseCase2.svg)

### 1.4 Pre-conditions
* The actor must be successfully authenticated via Auth0.
* The actor must possess a valid JWT signed with RS256, with at least 64 bits of entropy, carrying the `"Customer"` role claim.
* The JWT must not be expired (Customer sessions expire after 6 hours or 30 minutes of inactivity).

### 1.5 Post-conditions
* The list of available movies (with available stock) is successfully retrieved from the database.
* The information is returned to the actor in a structured JSON response containing movie titles, genres, prices, and stock quantities.
* An audit log entry is created recording the access to the movie catalog, including user ID, role, timestamp, and source IP.

---

## 2. Interaction Flow & Architecture
As the system is a backend-only API, the interaction follows a direct request-response pattern between the client and the server.

### 2.1 Interaction Flow (API Level)
1. **Request:** The Customer (via API Client) sends a `GET` request to `/api/movies` including the JWT in the `Authorization: Bearer` header. Optional query parameters (e.g., genre filter, search term, pagination) may be included.
2. **Authentication:** The `AuthMiddleware` verifies the JWT signature against Auth0's JWKS endpoint using RS256. Expired or malformed tokens are rejected with `401 Unauthorized`.
3. **Authorization:** The `RoleGuard` confirms the actor holds the `Customer` role. Requests from unrecognized roles are rejected with `403 Forbidden`.
4. **Input Validation:** Query parameters are validated server-side for type, format, and permitted range before reaching the business logic layer.
5. **Business Logic:** The `MovieController` invokes the `MovieService` to retrieve all movies with available stock from the repository.
6. **Response:** The system returns a `200 OK` status with the JSON array containing the movie catalog details.

### 2.2 Sequence Diagram
This diagram shows the internal backend logic and the sequence of calls between the Controller, Service, and Repository, highlighting the enforcement of security rules at the service layer.

![Sequence Diagram - UC2](./Diagrams/images/SequenceDiagram2.svg)

---

## 3. Threat Analysis
Specific threats to the process of browsing available movies were evaluated using STRIDE and Attack Trees. Threat IDs and risk scores reference the system-wide [Threat Model](../../ThreatModel/threatModel.md).

### 3.1 STRIDE Table
| Threat                                                                                        | Category                   | Mitigation Strategy                                                                                               |
|:----------------------------------------------------------------------------------------------|:---------------------------|:------------------------------------------------------------------------------------------------------------------|
| Attacker uses a forged or stolen JWT to impersonate a Customer (U1, B1 — Risk 12/10)          | **Spoofing**               | JWT signature validated against Auth0 JWKS (RS256) on every request; unsigned or malformed tokens rejected `401`. |
| Customer manually crafts requests targeting Support or Admin endpoints (U6 — Risk 12)         | **Elevation of Privilege** | `RoleGuard` enforces `Customer` role server-side; any role mismatch returns `403 Forbidden`.                      |
| Attacker injects malicious input via query parameters, e.g., search field (B3 — Risk 9)       | **Tampering**              | Server-side validation of all query parameters for type, format, and range before reaching `MovieService`.        |
| Verbose error responses expose stack traces or internal service details (B6 — Risk 12)        | **Information Disclosure** | Generic error messages returned to clients; internal identifiers and stack traces never included in responses.    |
| Attacker floods `GET /api/movies` to cause DoS or scrape full catalog data (U5, B7 — Risk 12) | **Denial of Service**      | Rate limiting middleware applied to the endpoint; `429` returned on threshold breach.                             |
| Unbounded catalog query with no pagination saturates database resources (D6 — Risk 9)         | **Denial of Service**      | Query result sets are paginated and limited server-side; requests without valid pagination parameters rejected.   |
| JWT or credentials intercepted in transit via network sniffing (U4 — Risk 8)                  | **Information Disclosure** | TLS enforced for all client-to-backend communications (ASVS V12.1.1); no plaintext fallback permitted.            |

---

## 4. Security Requirements (ASVS Compliance)
Based on the ASVS v5.0 checklist, the following requirements are most relevant to this UC:

* **ASVS V9.1.1 — L1 (Self-contained Tokens):** Self-contained tokens must be validated using their digital signature before 
any claim is trusted. The `AuthMiddleware` validates the JWT signature against Auth0's JWKS endpoint (RS256) before any claim 
such as `sub`, `role`, `exp`, or `iat` is trusted by the `MovieController` or `RoleGuard`. Expired, malformed, or unsigned tokens 
are rejected with `401 Unauthorized`.

* **ASVS V9.2.1 — L1 (Self-contained Tokens):** If a validity time span is present in the token, the token must be rejected if 
expired. Customer JWTs expire after 6 hours or 30 minutes of inactivity; the backend enforces the `exp` claim on every request to 
`GET /api/movies`, ensuring stale sessions cannot be reused.

* **ASVS V8.2.1 — L1 (Authorization):** Function-level access must be restricted to consumers with explicit authorization. 
The `RoleGuard` enforces that only JWTs carrying the `Customer` role can invoke `GET /api/movies`. Requests from unauthenticated 
users or other roles are rejected with `403 Forbidden` at the controller layer.

* **ASVS V2.2.1 — L1 (Validation and Business Logic):** Input must be validated to enforce business or functional expectations 
for each field. All query parameters (e.g., genre filter, search term, pagination values) are validated server-side for type, 
format, and permitted range before reaching `MovieService`. Only movies with available `StockQuantity` are returned, enforcing 
the domain invariant at the service layer.

* **ASVS V4.2.1 — L2 (API and Web Service):** Anti-automation controls must be in place to protect against excessive calls. 
Rate limiting is applied to `GET /api/movies` to prevent catalog scraping and denial-of-service conditions. Requests exceeding 
the defined threshold per time window are rejected with `429 Too Many Requests`.

* **ASVS V16.2.1 — L2 (Security Logging and Error Handling):** Each log entry must include necessary metadata such as when, 
where, who, and what. All access to the movie catalog endpoint is logged with timestamp, requesting user ID, assigned role, 
and source IP. Failed authentication and authorization attempts are captured as security-relevant events. Error responses 
never expose stack traces or internal service details to the client.

---

## 5. Secure Development Requirements
* **Code Review:** Any changes to the retrieval logic in `MovieService`, query parameter handling, or access rules in `RoleGuard` 
require a security-focused peer review, as defined in the project's secure development guidelines.

* **Automated Testing:** Unit and integration tests must cover:
  * Unauthenticated requests to `GET /api/movies` (expect `401`).
  * Requests with a valid JWT but a non-Customer role (expect `403`).
  * Requests with malformed or expired JWTs (expect `401`).
  * Requests with invalid query parameters (expect `400`).
  * Valid Customer requests returning only movies with available stock.

* **Dependency Management:** Libraries used in the movie retrieval pipeline are monitored for known vulnerabilities using 
automated tooling (e.g., Snyk, OWASP Dependency-Check) as part of the CI/CD pipeline.
