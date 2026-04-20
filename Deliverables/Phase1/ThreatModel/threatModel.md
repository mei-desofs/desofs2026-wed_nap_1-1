# Threat Model
___

## 1 Threat Model Information

* **Application Name**: eMovies Shop

* **Application Version**: 1.0

* **Description**: eMoviesShop is an application for browsing, purchasing (excluding payment integration), managing, and refunding physical copies of movies in a videoclub store. Customers can view available movies, place purchase orders and request refunds. Support users handle refund requests, while Admins oversee the movie catalog and manage user roles. Authentication and authorization are handled through an external identity provider (Auth0), which issues JWTs used to secure API requests and enforce role-based access control (RBAC). The backend validates these tokens to ensure proper access control across all operations. The system relies on HTTPS for secure communication and assumes a properly configured TLS-enabled server and network protections.

* **Document Owner**: DESOFT-2026-wed_nap_1

* **Participants**: Pedro Costa, Pedro Soares, Pedro Silva, Diogo Ribeiro, Miguel Cardoso

* **Reviewer**: Professor Paulo Baltarejo Sousa and Professor Nuno Pereira

___

## 2. External Dependencies

| ID | Description                                                                                                                                                                                                                                                                                                                     |
|----|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1  | **MySQL** is used as the relational database for storing users, movies, orders, and refund requests. It runs on a Linux-based server or managed cloud service, and must be secured with restricted access, regular patching, and encrypted connections.                                                                         |
| 2  | The **eMovie Shop Backend**, built in Node.js with Express, depends on the Node runtime and an ecosystem of external libraries. These must be actively maintained to avoid outdated packages introducing vulnerabilities.                                                                                                       |
| 3  | **GitHub** is used for source control, issue tracking, and CI/CD. GitHub Actions automate builds, security checks (e.g., Semgrep, Dependabot), and testing, forming a critical dependency in the SDLC.                                                                                                                          |
| 4  | All communication with the system occurs over HTTPS, with API requests being made directly through tools such as Postman. Authentication is handled via Bearer tokens (JWT) included in the request headers. This setup assumes a correctly configured TLS-enabled server and appropriate network protections (e.g., firewall). |
| 5  | The system manages sensitive data such as **user roles, purchase history, and refund requests**. This makes **secure database access and audit logging** essential for accountability and protection against data tampering.                                                                                                    |
| 6  | **Auth0** is used as an external Identity Provider for user authentication, and JWT issuance. The system depends on Auth0 for secure token generation and validation (e.g., via JWKS). Proper configuration of roles, claims, and token verification is required to prevent authentication and authorization flaws.             |

___

## 3. Entry Points

| ID       | Name                  | Description                                                                                                         | Trust Levels                                                  |
|----------|-----------------------|---------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------|
| 1        | HTTPS Port            | All access to eMovie Shop is served via HTTPS (TLS) through direct API requests.                                    | (1) Guest, (2) Invalid Credentials, (3) Authenticated User    |
| *1.1*    | Authentication        | Users authenticate via an external Identity Provider (Auth0) to access protected features.                          | (1) Guest, (2) Invalid Credentials, (3) Authenticated Users   |
| *1.1.1*  | Login Function        | Users submit credentials to Auth0 and receive a signed JWT on success. This token must be included in API calls. Login endpoints are rate-limited to 30 requests/min per IP.    | (4) Customer, (5) Support, (6) Admin                          |
| 2        | API Endpoints         | All the system functionalities are exposed via REST API endpoints (e.g., /movies, /library). Baseline abuse controls: 120 requests/min per authenticated user and 300 requests/min per IP.                         | (4) Customer                                                  |
| *2.1*    | Browse Movies         | Public endpoint to view movies. API call supports filtering and pagination.                                         | (4) Customer                                                  |
| *2.2*    | Purchase Movie        | Authenticated customers may trigger a purchase via a backend API call.                                              | (4) Customer                                                  |
| *2.3*    | View Library          | Customers can view previously purchased movies and refund-eligible items.                                           | (4) Customer                                                  |
| *2.4*    | Request Refund        | Triggers refund request for a specific OrderItem. Request payload is constrained by API size limits (1 MB global, 256 KB refund endpoint).                                                                  | (4) Customer                                                  |
| 3        | Support Tools         | API endpoints intended for support-level operations.                                                                | (5) Support                                                   |
| *3.1*    | View Refund Requests  | Support users can list and approve/reject refunds.                                                                  | (5) Support                                                   |
| 4        | Admin Console         | API endpoints reserved for administrative operations.                                                               | (6) Admin                                                     |
| *4.1*    | Manage Movie Catalog  | Add/edit/remove movies and update prices or stock.                                                                  | (6) Admin                                                     |
| *4.2*    | Manage User Roles     | Assign support/admin roles to registered users.                                                                     | (6) Admin                                                     |

___

## 4. Exit Points

| ID      | Name                         | Description                                                                                                      | Trust Levels                                                 |
|---------|------------------------------|------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------|
| 1       | HTTPS Responses              | All content is delivered via HTTPS, including API responses, HTML, JSON, and static assets.                      | (1) Guest, (2) Invalid Credentials, (3) Authenticated User   |
| *1.1*   | Login Response               | Authentication is handled by Auth0, which returns a signed JWT or an error. The backend only validates tokens.   | (1) Guest, (2) Invalid Credentials, (3) Authenticated User   |
| *1.2*   | Error Responses              | The system returns appropriate HTTP status codes and error messages(e.g., 400, 401, 403, 404, 409, 429, 500). Error payloads are sanitized and include a correlation ID for investigation.                   | All trust levels                                             |
| 2       | Customer Responses           | Data shown to authenticated customers.                                                                           | (4) Customer                                                 |
| *2.1*   | Movie List                   | The customer sees a list of available movies, fetched via API.                                                   | (4) Customer                                                 |
| *2.2*   | Purchase Confirmation        | After initiating a purchase, the server returns confirmation or validation errors.                               | (4) Customer                                                 |
| *2.3*   | Library / Owned Movies       | The customer can view a list of previously purchased movies.                                                     | (4) Customer                                                 |
| *2.4*   | Refund Request Result        | After submitting a refund request, the response includes confirmation or rejection message.                      | (4) Customer                                                 |
| 3       | Support Responses            | Responses sent to support users managing refund flows.                                                           | (5) Support                                                  |
| *3.1*   | Refund List API              | A list of pending refund requests is returned to the support dashboard.                                          | (5) Support                                                  |
| *3.2*   | Refund Decision Result       | Confirmation/error response after approving or rejecting a refund request.                                       | (5) Support                                                  |
| 4       | Admin Responses              | Responses related to administrative operations.                                                                  | (6) Admin                                                    |
| *4.1*   | Catalog Update Result        | Confirmation/error after adding/editing/deleting a movie in the catalog.                                         | (6) Admin                                                    |
| *4.2*   | Role Assignment Feedback     | Confirmation or errors when assigning roles to users.                                                            | (6) Admin                                                    |

___

## 5. Assets

| ID    | Name                                      | Description                                                                                                                                                                                                                         | Trust Levels                                                                        |
|-------|-------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------|
| **1** | **eMovie Shop Users**                     | **Assets related to customers, support staff, and administrators.**                                                                                                                                                                 |                                                                                     |
| 1.1   | User Identity Data                        | User identity attributes (e.g., email, roles, identifiers) managed via Auth0 and used by the system.                                                                                                                                | (3) Authenticated User                                                              |
| 1.2   | User Roles & Claims                       | Role information (e.g., CUSTOMER, SUPPORT, ADMIN) included in JWT claims and used for RBAC enforcement.                                                                                                                             | (3) Authenticated User                                                              |
| 1.3   | JWT Session Token                         | JWT issued by Auth0 and validated by the backend for authentication and authorization.                                                                                                                                              | (3) Authenticated User                                                              |
| 1.4   | Personal Data                             | Includes name, email, purchase history, refund requests. Must remain private.                                                                                                                                                       | (4) Customer, (5) Support, (6) Admin                                                |
| **2** | **System Availability & Access**          | **Assets related to the application’s infrastructure and runtime system.**                                                                                                                                                          |                                                                                     |
| 2.1   | Availability of the Backend               | The Node.js backend must be online and responsive 24/7 to process API requests.                                                                                                                                                     | (7) System Administrator                                                            |
| 2.2   | Availability of the Database              | The MySQL database must be accessible to store and retrieve business-critical data.                                                                                                                                                 | (8) Database Administrator                                                          |
| 2.3   | Performance of the Backend                | Backend must respond in <1.5s for refund/purchase actions.                                                                                                                                                                          | (7) System Administrator                                                            |
| 2.4   | Ability to execute API Commands           | Ability to perform operations through exposed APIs (e.g., buy, refund, assign roles).                                                                                                                                               | (4) Customer, (5) Support, (6) Admin                                                |
| 2.5   | Ability to execute SQL on the Database    | Full access to database manipulation: insert, update, delete, select.                                                                                                                                                               | (8) Database Administrator, (9) Database Read User, (10) Database Read & Write User |
| 2.6   | Code execution in backend environment     | Ability to run code (scripts, API logic) in the backend server environment.                                                                                                                                                         | (7) System Administrator                                                            |
| 2.7   | API Availability and Routing              | Internal infrastructure must route API requests securely and consistently.                                                                                                                                                          | (7) System Administrator                                                            |
| **3** | **Orders and Refunds**                    | **Assets tied to the purchasing and refund workflow.**                                                                                                                                                                              |                                                                                     |
| 3.1   | Order Records                             | Immutable records of movie purchases (items, total, date, status).                                                                                                                                                                  | (4) Customer (view), (6) Admin (view, audit)                                        |
| 3.2   | Refund Requests                           | Requests tied to order items, containing reason, date, and status.                                                                                                                                                                  | (4) Customer (create/view), (5) Support (review)                                    |
| 3.3   | Refund Decisions                          | Actions taken on refunds (approve/reject) and associated audit trail.                                                                                                                                                               | (5) Support (approve/reject), (6) Admin (audit/override)                            |
| 3.4   | Order Status Logic                        | Internal state (`PENDING`, `COMPLETED`, `REFUNDED`) maintained by backend logic.                                                                                                                                                    | (5) Support (triggered), (6) Admin (monitor/override)                               |
| **4** | **Movie Catalog & Inventory**             | **Assets related to published movie content and stock.**                                                                                                                                                                            |                                                                                     |
| 4.1   | Movie Metadata                            | Movie title, genre, description, and age rating shown to users.                                                                                                                                                                     | (4) Customer (view), (6) Admin (edit)                                               |
| 4.2   | Pricing and Discount Info                 | Price must be ≤ €500. Discounts managed by Admin.                                                                                                                                                                                   | (6) Admin                                                                           |
| 4.3   | Stock Quantity                            | Number of copies available; decremented on purchase.                                                                                                                                                                                | (6) Admin                                                                           |
| **5** | **Application Platform**                  | **Assets comprising application code, access logic, and interfaces.**                                                                                                                                                               |                                                                                     |
| 5.1   | Backend REST API                          | NodeJS application processing requests and enforcing RBAC.                                                                                                                                                                          | (3) Authenticated User                                                              |
| 5.2   | API Access Interface                      | External clients (e.g., Postman, curl) making HTTP requests with Authorization: Bearer <JWT>                                                                                                                                        | (3) Authenticated User                                                              |
| 5.3   | Auth Middleware and RoleGuard             | Backend enforcement of RBAC & auth before hitting business logic.                                                                                                                                                                   | (6) Admin (uses it), (7) System Admin (configures and maintains it)                 |
| **6** | **Monitoring, Logging, and Compliance**   | **Security and operational observability tools.**                                                                                                                                                                                   |                                                                                     |
| 6.1   | Audit Logs                                | Tracks business-critical events like refunds, purchases, and role changes. Visible to Admins; managed by System Admins.                                                                                                             | (6) Admin (view), (7) System Admin (manage)                                         |
| 6.2   | Logging Mechanism                         | Backend logs that trace errors or events; excludes sensitive data.                                                                                                                                                                  | (7) System Admin                                                                    |
| 6.3   | Alerting & Metrics                        | Health checks, uptime metrics, alerts for system issues.                                                                                                                                                                            | (7) System Admin                                                                    |
| **7** | **Organizational & Legal**                | **Company image, policies, and user-facing terms.**                                                                                                                                                                                 |                                                                                     |
| 7.1   | Brand Reputation                          | eMovie Shop reputation depends on trust, uptime, and fair policies.                                                                                                                                                                 | All trust levels                                                                    |
| 7.2   | Refund Policy                             | Refunds allowed for completed orders within 14 days only.                                                                                                                                                                           | (4) Customer (submit), (6) Admin (define policy)                                    |
| 7.3   | Terms & Conditions                        | Legal documents tied to user behavior and obligations.                                                                                                                                                                              | (1) Guest, (3) Authenticated User                                                   |
| 8     | External Services                         | Third-party integrations.                                                                                                                                                                                                           |                                                                                     |
| 8.1   | Identity Provider (Auth0)                 | External authentication and authorization service responsible for user registration, login, and JWT issuance. Must ensure secure token issuance, signature validation (e.g., JWKS), and correct configuration of roles and claims.. | (1) Guest, (2) Invalid Credentials, (3) Authenticated User                          |

___

## 6. Trust Levels

| ID       | Name                         | Description                                                                                                              |
|----------|------------------------------|--------------------------------------------------------------------------------------------------------------------------|
| **1**    | Guest                        | An unauthenticated actor interacting with the eMovieShop without logging in. Limited to publicly accessible endpoints.  |
| **2**    | Invalid Credentials          | Actor that failed authentication via Auth0 and does not possess a valid JWT.                                             |
| **3**    | Authenticated User           | A user authenticated via Auth0 with a valid JWT. Includes all roles: Customer, Support, or Admin.                        |
| **4**    | Customer                     | A user with the `CUSTOMER` role. Can browse movies, make purchases, and request refunds.                                 |
| **5**    | Support                      | A user with the `SUPPORT` role. Can view, approve, or reject refund requests.                                            |
| **6**    | Admin                        | A user with the `ADMIN` role. Manages movies, stock, discounts, and assigns user roles.                                  |
| **7**    | System Administrator         | Person responsible for infrastructure, deployment, system monitoring, and security configuration.                        |
| **8**    | Database Administrator       | Full access to the MySQL database. Can create/modify tables, manage credentials, and ensure data integrity.              |
| **9**    | Database Read User           | A user or service with read-only access to the database (e.g., analytics or report generation).                          |
| **10**   | Database Read & Write User   | A user or service with privileges to query and modify records, but not manage the database schema.                       |

___

## 7. Data Flow Diagrams

### 7.1. DFD - Level 0

![DFD-Level0.svg](resources/DFD-Level0.svg)

This Level 0 Data Flow Diagram illustrates the major interactions between external users (Customer, Support, Admin) and the eMovie Shop system. The system is represented as a single process, encapsulating the backend and database layers. 

Each user interacts with the system through distinct data flows that correspond to specific business actions, such as browsing movies, purchasing, handling refunds, or managing roles and movie catalog entries.
This diagram establishes the system boundary and highlights the trust relationships that will be explored further in the Level 1 DFD and threat analysis.

### 7.2. DFD - Level 1

![DFD-Level1.svg](resources/DFD-Level1.svg)

This Level 1 DFD decomposes the internal structure of the eMovie Shop system, showing detailed flows between backend, database and external services.

- **Users** (Customer, Support, Admin) interact directly with the **Backend** via HTTPS using tools such as Postman. All operations (e.g., login, purchases, refunds) are performed through secured API calls with JWT Bearer tokens.
- The **Backend** processes logic and persists data into a **MySQL database**, handling movie orders, refunds, and user management.
- On authentication-related actions (e.g., user login), the backend communicates with an external Authentication Service (e.g., Auth0). User credentials are securely forwarded to the external service, which validates them and returns an access token (JWT).
   * This interaction is represented as a bidirectional data flow between the backend and the external authentication provider.
- Red dashed lines indicate internal **trust boundaries**, while gray dashed lines represent **external communications** beyond system control.

This decomposition helps clarify integration points, potential exit paths, and responsibilities of each core component — which is especially useful for threat modeling and secure architecture analysis.

___

## 2. Determining and Ranking Threats

### 2.1. Threat Categorization

eMovie Shop follows an attacker-centric approach to threat modeling. Threats are categorized using the **STRIDE** methodology to ensure coverage across security properties such as authentication, integrity, confidentiality, and availability.

| STRIDE Category              | Violated Property   | Description                                         |
|------------------------------|---------------------|-----------------------------------------------------|
| **Spoofing**                 | Authentication      | Pretending to be another user or component          |
| **Tampering**                | Integrity           | Unauthorized modification of data or code           |
| **Repudiation**              | Non-repudiation     | Denial of an action due to a lack of accountability |
| **Information Disclosure**   | Confidentiality     | Exposure of sensitive data to unauthorized parties  |
| **Denial of Service**        | Availability        | Resource exhaustion or service disruption           |
| **Elevation of Privilege**   | Authorization       | Unauthorized permission escalation                  |

___

### 2.2. Threat Analysis

#### 2.2.1. STRIDE

**API Clients → Backend (User / Backend Boundary)**

| ID     | Category                   | Description                                                                                                                                                     |
|--------|----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| U1     | Spoofing                   | An attacker submits forged or stolen JWTs in the `Authorization` header to impersonate a legitimate user on any protected endpoint.                             |
| U2     | Spoofing                   | An attacker performs brute-force or credential stuffing against `POST /auth/login`, attempting to obtain valid credentials.                                     |
| U3     | Tampering                  | A malicious actor modifies the request body in transit (e.g., altering a refund request payload) if TLS is not enforced end-to-end.                             |
| U4     | Information Disclosure     | Credentials or JWT tokens are intercepted via network sniffing if the connection between the API client and the backend is not protected by TLS.                |
| U5     | Denial of Service          | An attacker floods the backend with repeated requests to resource-intensive endpoints (e.g., `GET /movies`, `POST /auth/login`), causing service degradation.   |
| U6     | Elevation of Privilege     | A Customer role user manually crafts requests to support or admin endpoints (e.g., `GET /refunds`, `PATCH /movies/:id`) bypassing client-side restrictions.     |

**Backend (Node.js API)**

| ID     | Category                 | Description                                                                                                                                                                   |
|--------|--------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| B1     | Spoofing                 | The backend accepts manipulated or weakly validated JWTs due to improper signature verification, allowing an attacker to forge identity claims.                               |
| B2     | Spoofing                 | The Auth0 `client_secret` is exposed (e.g., hardcoded in source control), allowing an attacker to forge token requests directly against Auth0.                                |
| B3     | Tampering                | Missing input sanitization in fields (eg., refund reasons. movie descriptions) allows stored content injection that corrupts data integrity.                                  |
| B4     | Tampering                | CORS misconfiguration or improper `Authorization` header handling allows cross-origin requests to abuse public or semi-protected endpoints.                                   |
| B5     | Repudiation              | Absence of audit logging for sensitive operations (refund approval/rejection, role changes, movie catalog edits) makes it impossible to attribute actions to specific actors. |
| B6     | Information Disclosure   | Overly verbose error messages or unfiltered API responses expose internal details (e.g., stack traces, user roles, internal IDs) to unauthorized parties.                     |
| B7     | Denial of Service        | Lack of rate limiting on resource-intensive endpoints (eg., login ,refund submission) allows automated abuse that saturates the backend API.                                  |
| B8     | Elevation of Privilege   | A route lacks a `RoleGuard` or the guard is misconfigured, allowing a lower-privilege actor to invoke operations outside their permitted scope.                               |

**Backend → Auth0 (Backend / External Authentication Service Boundary)**

| ID     | Category                 | Description                                                                                                                                                                           |
|--------|--------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| A1     | Spoofing                 | An attacker intercepts the ROPC token request between the backend and Auth0 (e.g., via a compromised network path) and replays captured credentials.                                  |
| A2     | Spoofing                 | Auth0 tenant misconfiguration (e.g., ROPC grant disabled, wrong audience) causes authentication failures that an attacker could exploit to force fallback mechanisms.                 |
| A3     | Tampering                | The `client_id`, `client_secret`, or `audience` values are tampered with in the backend environment, redirecting authentication requests to an attacker-controlled identity provider. |
| A4     | Information Disclosure   | The ROPC token request payload containing `username` and `password` is exposed if the backend-to-Auth0 communication is not enforced over TLS.                                        |
| A5     | Denial of Service        | An attacker triggers repeated failed authentication attempts causing Auth0's rate limiter or anomaly detection to lock out legitimate users.                                          |
| A6     | Elevation of Privilege   | A leaked Auth0 `client_secret` allows an attacker to obtain tokens for arbitrary users directly from Auth0, bypassing the backend entirely.                                           |

**Backend → Database (Backend / Database Boundary)**

| ID     | Category                 | Description                                                                                                                                                                       |
|--------|--------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| D1     | Spoofing                 | Backend services running with overly permissive database accounts can issue unauthorized queries, effectively impersonating a higher-privilege database role.                     |
| D2     | Tampering                | SQL injection via unsanitized parameters in endpoints such as refund submission or movie catalog management alters or deletes critical records.                                   |
| D3     | Tampering                | A compromised backend service with write access beyond its scope modifies records (e.g., order status, refund decisions) without proper authorization checks.                     |
| D4     | Repudiation              | Absence of database-level audit logging makes it impossible to trace unauthorized or malicious inserts, updates, or deletions to a specific actor or session.                     |
| D5     | Information Disclosure   | Insufficient row-level access controls in database queries expose purchase records, refund data, or user information belonging to other customers.                                |
| D6     | Denial of Service        | Unbounded or unpaginated queries (e.g., fetching the full movie catalog or all refund records without limits) saturate database resources and degrade overall system performance. |

___

#### 2.2.2. Attack Trees

| **Attack Tree Scenario**                   | **UC(s) Covered**    | **STRIDE Category Covered**                              |
|--------------------------------------------|----------------------|----------------------------------------------------------|
| 1. **Credential Access**                   | UC1 (Login)          | Spoofing, Information Disclosure                         |
| 2. **Manipulate Refund Process**           | UC4, UC5, UC6        | Tampering, Repudiation, Elevation of Privilege           |
| 3. **Unauthorized Admin Access**           | UC7, UC8             | Elevation of Privilege, Spoofing                         |
| 4. **Steal/Leak Personal Data**            | UC1–UC6              | Information Disclosure, Repudiation                      |
| 5. **Disrupt System Availability (DoS)**   | UC1–UC6              | Denial of Service                                        |
| 6. **Exploit Role Change Mechanism**       | UC8                  | Tampering, Spoofing, Elevation of Privilege, Repudiation |


* **Attack Tree - Unauthorized Access**

![Attack Tree - Unauthorized Access](resources/Attack%20Tree-1.svg)

* **Attack Tree - Manipulate Refund Process**

![Attack Tree-2.svg](resources/Attack%20Tree-2.svg)

* **Attack Tree - Unauthorized Admin Access**

![Attack Tree-3.svg](resources/Attack%20Tree-3.svg)

* **Attack Tree - Steal/Leak Personal Data**

![Attack Tree-4.svg](resources/Attack%20Tree-4.svg)

* **Attack Tree - Disrupt System Availability (DoS)**

![Attack Tree-5.svg](resources/Attack%20Tree-5.svg)

* **Attack Tree - Exploit Role Change Mechanism**

![Attack Tree-6.svg](resources/Attack%20Tree-6.svg)

___

#### 2.2.3. Use/Abuse Cases

The Use/Abuse Cases are shown in the individuals reports of each use case.
* UC1: [Use/Abuse Case UC1](../UseCases/UC1_Login/README.MD)
* UC2: [Use/Abuse Case UC2](../UseCases/UC2_ViewAvailableMovies/README.MD)
* UC3: [Use/Abuse Case UC3](../UseCases/UC3_PurchaseMovie/README.md)
* UC4: [Use/Abuse Case UC4](../UseCases/UC4_RequestRefund/README.md)
* UC5: [Use/Abuse Case UC5](../UseCases/UC5_ViewRequestRefunds/README.md)
* UC6: [Use/Abuse Case UC6](../UseCases/UC6_HandleRefundRequest/README.md)
* UC7: [Use/Abuse Case UC7](../UseCases/UC7_ManageMovieCatalog/README.MD)
* UC8: [Use/Abuse Case UC8](../UseCases/UC8_ManageRoles/README.md)

___

#### 2.2.4. Other threats (Threat catalogs)

___

### 2.3. Ranking of Threats

To assess and prioritize the identified threats in eMovie Shop, we apply a **Qualitative Risk Model** based on two key dimensions:

- **Cost (Impact):** The severity of the consequence if the threat is exploited.
- **Probability (Likelihood):** The estimated frequency or feasibility of the threat occurring.

Each threat is assigned a score from 1 (lowest) to 5 (highest) in both dimensions. The **risk value** is calculated by multiplying the cost and probability:

> **Risk = Cost × Probability**

| Cost           | Value   | Description                                                             |
|----------------|---------|-------------------------------------------------------------------------|
| Negligible     | 1       | Minor issue with no impact on users or business.                        |
| Minor          | 2       | Temporary inconvenience, affects only non-critical functionality.       |
| Moderate       | 3       | Breach of limited data or functionality, requires attention.            |
| Major          | 4       | Exposes important data or system features, damages trust or operations. |
| Catastrophic   | 5       | Systemic failure, major data leak, or critical business impact.         |

| Probability     | Value   | Description                                                                |
|-----------------|---------|----------------------------------------------------------------------------|
| Very Unlikely   | 1       | Highly unlikely or rare, requires advanced tools or physical access.       |
| Unlikely        | 2       | Possible under certain conditions, low incentive or difficulty exploiting. |
| Possible        | 3       | Realistic threat, mitigated by existing controls, but still exploitable.   |
| Likely          | 4       | Frequently seen in similar systems, moderate technical skill required.     |
| Very Likely     | 5       | Actively exploited in the wild or trivially exploitable in current setup.  |

Based on the qualitative model, the following risk scores have been calculated for the identified threats in eMovie Shop:

**API Clients → Backend (User / Backend Boundary)**

| ID    | Threat                                        | Category                 | Cost   | Probability   | Risk   |
|-------|-----------------------------------------------|--------------------------|--------|---------------|--------|
| U1    | Forged/stolen JWT used to impersonate user    | Spoofing                 | 4      | 3             | 12     |
| U2    | Brute-force / credential stuffing on login    | Spoofing                 | 4      | 4             | 16     |
| U3    | Request body tampered in transit              | Tampering                | 3      | 2             | 6      |
| U4    | Credentials/JWT intercepted via sniffing      | Information Disclosure   | 4      | 2             | 8      |
| U5    | Endpoint flooding causing DoS                 | Denial of Service        | 3      | 4             | 12     |
| U6    | Customer crafts requests to admin endpoints   | Elevation of Privilege   | 4      | 3             | 12     |

**Backend (Node.js API)**

| ID    | Threat                                               | Category                 | Cost   | Probability   | Risk   |
|-------|------------------------------------------------------|--------------------------|--------|---------------|--------|
| B1    | Forged JWT accepted due to weak validation           | Spoofing                 | 5      | 2             | 10     |
| B2    | Auth0 client_secret exposed in source control        | Spoofing                 | 5      | 3             | 15     |
| B3    | Missing input sanitization allows data injection     | Tampering                | 3      | 3             | 9      |
| B4    | CORS misconfiguration abuses endpoints               | Tampering                | 3      | 2             | 6      |
| B5    | No audit log for sensitive operations                | Repudiation              | 4      | 3             | 12     |
| B6    | Verbose errors expose internal details               | Information Disclosure   | 3      | 4             | 12     |
| B7    | No rate limiting on on resource-intensive endpoints  | Denial of Service        | 3      | 4             | 12     |
| B8    | Missing/misconfigured RoleGuard on route             | Elevation of Privilege   | 5      | 3             | 15     |

**Backend → Auth0 (External Authentication Service Boundary)**

| ID    | Threat                                               | Category                | Cost  | Probability   | Risk   |
|-------|------------------------------------------------------|-------------------------|-------|---------------|--------|
| A1    | ROPC request intercepted and credentials replayed    | Spoofing                | 4     | 2             | 8      |
| A2    | Auth0 tenant misconfiguration exploited              | Spoofing                | 4     | 2             | 8      |
| A3    | client_id/secret tampered to redirect to rogue IdP   | Tampering               | 5     | 1             | 5      |
| A4    | ROPC payload exposed if TLS not enforced to Auth0    | Information Disclosure  | 4     | 2             | 8      |
| A5    | Repeated failures trigger Auth0 lockout of users     | Denial of Service       | 3     | 3             | 9      |
| A6    | Leaked client_secret allows direct Auth0 token req   | Elevation of Privilege  | 5     | 2             | 10     |

**Backend → Database (Backend / Database Boundary)**

| ID    | Threat                                               | Category                 | Cost   | Probability   | Risk   |
|-------|------------------------------------------------------|--------------------------|--------|---------------|--------|
| D1    | Overpermissive DB account impersonates higher role   | Spoofing                 | 4      | 2             | 8      |
| D2    | SQL injection via unsanitized parameters             | Tampering                | 5      | 3             | 15     |
| D3    | Compromised backend modifies records out of scope    | Tampering                | 4      | 2             | 8      |
| D4    | No DB audit log, actions untraceable                 | Repudiation              | 4      | 3             | 12     |
| D5    | Row-level controls missing, data cross-exposure      | Information Disclosure   | 4      | 3             | 12     |
| D6    | Unbounded queries saturate database resources        | Denial of Service        | 3      | 3             | 9      |

___

## 3. Determining Countermeasures and Mitigation

### 3.1. STRIDE Mitigation & Countermeasures Techniques

#### 3.3.1 STRIDE Mitigation & Countermeasures Techniques

| **Threat ID**   | **Description**                                                  | **Countermeasures**                                                                                                                                                                                                                          |
|-----------------|------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| U1              | Forged/stolen JWT used to impersonate a legitimate user          | Validate JWT signature, issuer, audience, and expiry on every request; Use short-lived tokens; Implement token revocation via Auth0 blacklisting.                                                                                            |
| U2              | Brute-force / credential stuffing against the login endpoint     | Enforce rate limiting and account lockout after repeated failures; Enable Auth0 Attack Protection (brute-force and breached password detection).                                                                                             |
| U3              | Request body tampered in transit                                 | Enforce TLS (HTTPS) for all client-to-backend communication; Reject requests over unencrypted channels; Apply HSTS headers.                                                                                                                  |
| U4              | Credentials or JWT intercepted via network sniffing              | Enforce TLS end-to-end; Never transmit credentials or tokens over plain HTTP; Apply HSTS to prevent protocol downgrade attacks.                                                                                                              |
| U5              | Endpoint flooding causing backend Denial of Service              | Apply rate limiting middleware on all endpoints; Implement per-IP and per-user throttling; Return `429 Too Many Requests` with retry-after headers.                                                                                          |
| U6              | Customer crafts requests to support/admin-only endpoints         | Enforce server-side RBAC via `RoleGuard` on every protected route; Never rely on client-supplied role claims without server-side verification.                                                                                               |
| B1              | Forged JWT accepted due to weak or missing validation            | Use RS256 or HS512 signing algorithms; Strictly validate `iss`, `aud`, `exp`, and `sub` claims on every request; Reject tokens with unexpected claims.                                                                                       |
| B2              | Auth0 `client_secret` exposed in source control                  | Store `client_id`, `client_secret`, and `audience` exclusively in environment variables; Integrate secrets scanning (e.g., GitGuardian, GitHub secret scanning) into the CI/CD pipeline; Rotate secrets immediately upon suspected exposure. |
| B3              | Missing input sanitization allows data injection                 | Sanitize and validate all user-supplied input on the backend; Use an ORM with parameterized queries; Reject inputs that exceed expected length or type.                                                                                      |
| B4              | CORS misconfiguration abuses semi-protected endpoints            | Restrict CORS to explicitly trusted origins; Validate the `Authorization` header presence and format on all protected routes; Disable wildcard CORS in production.                                                                           |
| B5              | No audit log for sensitive operations                            | Implement structured audit logging for all sensitive actions (login attempts, refund decisions, role changes, catalog edits); Include timestamp, actor ID, source IP, and outcome; Store logs in an append-only store.                       |
| B6              | Verbose error messages expose internal implementation            | Return generic error messages in production; Disable stack traces in API responses; Use centralized error handling middleware that sanitizes output before returning.                                                                        |
| B7              | No rate limiting on on resource-intensive endpoints              | Apply rate limiting middleware on resource-intensive endpoints; Track failed attempts per IP and per account; Lock out after threshold is exceeded.                                                                                          |
| B8              | Missing or misconfigured `RoleGuard` on a route                  | Enforce `RoleGuard` on every non-public endpoint; Apply deny-by-default access control (reject unless explicitly permitted); Include integration tests that assert unauthorized access returns `403 Forbidden`.                              |
| A1              | ROPC token request intercepted and credentials replayed          | Enforce TLS on all backend-to-Auth0 communication; Use short-lived tokens with `expires_in`; Validate token freshness on every use.                                                                                                          |
| A2              | Auth0 tenant misconfiguration exploited by attacker              | Regularly audit Auth0 tenant settings (ROPC grant status, allowed audiences, connection policies); Enable Auth0 anomaly detection and alert on configuration changes.                                                                        |
| A3              | client_id/secret tampered to redirect auth to rogue IdP          | Store Auth0 configuration exclusively in server-side environment variables; Restrict environment access to authorized personnel; Verify the Auth0 token issuer (`iss`) on every JWT validation.                                              |
| A4              | ROPC payload exposed if TLS not enforced toward Auth0            | Enforce HTTPS for all outbound requests to Auth0; Validate the Auth0 endpoint certificate; Never allow self-signed certificates in the token exchange path.                                                                                  |
| A5              | Repeated failures trigger Auth0 lockout of legitimate users      | Enable Auth0 brute-force protection with appropriate thresholds; Implement backend-side rate limiting before the request reaches Auth0 to absorb abuse early.                                                                                |
| A6              | Leaked `client_secret` allows direct token requests to Auth0     | Apply the same secret management controls as B2; Scope the Auth0 application to the minimum required grants; Monitor Auth0 logs for token issuance from unexpected sources.                                                                  |
| D1              | Overpermissive DB account impersonates higher-privilege role     | Apply least-privilege principles to database accounts; Use separate accounts per service role (read-only for browsing, write for orders); Restrict direct DB access via firewall rules.                                                      |
| D2              | SQL injection via unsanitized parameters                         | Use an ORM with parameterized queries exclusively; Validate and type-check all inputs before they reach the data layer; Run static analysis tools (e.g., SonarQube) to detect injection-prone patterns.                                      |
| D3              | Compromised backend modifies records outside its scope           | Enforce application-level ownership checks before any write operation (e.g., `WHERE user_id = ?`); Use separate DB roles with scoped write permissions per domain.                                                                           |
| D4              | No database audit log, actions untraceable                       | Log all CRUD operations through the backend service layer with actor ID, timestamp, and affected record; Protect log integrity with append-only storage or external log forwarding.                                                          |
| D5              | Row-level controls missing, data cross-exposure                  | Apply `WHERE user_id = ?` filters on all user-scoped queries; Enforce ownership checks at the service layer before returning any record; Conduct security-focused code reviews on data access methods.                                       |
| D6              | Unbounded queries saturate database resources                    | Enforce mandatory pagination on all list endpoints; Apply query limits at the ORM layer; Add performance and load tests to detect unbounded query patterns.                                                                                  |
___

### 3.2. Security Test Planning

#### 3.3.2 Security Test Planning

| **Countermeasure(s)**                                                                                | **Test Planning**                                                                                                                                                                                          |
|------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Enforce server-side RBAC via `RoleGuard` on every protected route                                    | Using Postman, send requests to support/admin-only endpoints (e.g., `GET /refunds`, `PATCH /movies/:id`) with a Customer-role JWT and verify a `403 Forbidden` response is returned.                       |
| Validate JWT signature, issuer, audience, and expiry on every request                                | Tamper with a valid JWT (modify payload claims or signature) and submit it to a protected endpoint; verify the backend rejects it with `401 Unauthorized`.                                                 |
| Use RS256 or HS512; strictly validate `iss`, `aud`, and `exp` claims                                 | Submit an expired token, a token with an incorrect `aud`, and a token signed with a different key; confirm all are rejected with appropriate error responses.                                              |
| Enforce rate limiting and account lockout on `POST /auth/login`                                      | Simulate repeated failed login attempts (e.g., using a Postman collection runner or a script) and verify lockout or `429 Too Many Requests` is triggered after the defined threshold.                      |
| Enable Auth0 Attack Protection (brute-force and breached password detection)                         | In the Auth0 dashboard, verify that brute-force protection is active; trigger the threshold and confirm Auth0 blocks further attempts and logs the event.                                                  |
| Enforce TLS end-to-end; apply HSTS; reject plain HTTP requests                                       | Attempt to call the API over plain HTTP and verify it is either rejected or redirected; inspect response headers with `curl -I` to confirm `Strict-Transport-Security` is present.                         |
| Store `client_secret` and Auth0 config in environment variables; integrate secrets scanning in CI/CD | Inspect the source code repository and build artifacts to confirm no credentials are hardcoded; run a secrets scanning tool (e.g., GitGuardian or `git-secrets`) against the commit history.               |
| Sanitize and validate all user-supplied input on the backend                                         | Inject malformed or oversized payloads into fields such as refund reason or movie description and verify the backend returns a validation error without persisting or reflecting the input.                |
| Use ORM with parameterized queries; run static analysis for injection patterns                       | Attempt SQL injection via query parameters and request body fields (e.g., `' OR 1=1 --`); verify the ORM prevents execution; run SonarQube or Semgrep and confirm no injection-prone patterns are flagged. |
| Disable wildcard CORS; restrict to explicitly trusted origins                                        | Send API requests from an untrusted origin using a modified `Origin` header and verify the server returns a CORS error; confirm no wildcard `Access-Control-Allow-Origin` header is present.               |
| Return generic error messages in production; use centralized error handling                          | Use OWASP ZAP or Postman to trigger server errors (e.g., malformed requests, invalid IDs) and verify that responses contain no stack traces, file paths, or internal identifiers.                          |
| Implement structured audit logging for all sensitive actions                                         | Trigger sensitive operations (login, refund approval/rejection, role change, catalog edit) and inspect the audit log store to verify each entry includes timestamp, actor ID, source IP, and outcome.      |
| Apply rate limiting on all endpoints; enforce per-IP and per-user throttling                         | Flood a resource-intensive endpoint (e.g., `GET /movies`, `POST /refunds`) with rapid successive requests and verify `429` responses are returned and normal traffic recovers after the window expires.    |
| Enforce mandatory pagination on all list endpoints; apply ORM query limits                           | Call list endpoints (e.g., `GET /movies`, `GET /refunds`) without pagination parameters and verify the backend enforces a maximum result limit; confirm no unbounded query reaches the database.           |
| Apply least-privilege DB accounts; restrict permissions per service role                             | Attempt write operations using a read-only database account (via direct DB client or backend inspection) and verify they are rejected; review DB user grants to confirm minimum privilege is applied.      |
| Apply `WHERE user_id = ?` ownership filters on all user-scoped queries                               | Authenticate as Customer A and attempt to retrieve or modify records belonging to Customer B (e.g., orders, refunds) by manipulating IDs in the request; verify the backend returns `403` or `404`.        |
| Enforce HTTPS for all backend-to-Auth0 communication; validate Auth0 endpoint certificate            | Inspect outbound requests from the backend to Auth0 (e.g., via proxy or integration test logs) and confirm all calls use HTTPS with a valid certificate; verify no fallback to HTTP exists.                |
| Monitor Auth0 logs for token issuance from unexpected sources                                        | In the Auth0 dashboard, review the log stream after normal operation and after a simulated `client_secret` leak scenario; verify alerts are triggered for anomalous token issuance patterns.               |
| Enforce application-level ownership checks before any write operation                                | Submit PATCH/DELETE requests targeting records not owned by the authenticated user (varying IDs in the path); verify the service layer rejects the operation before it reaches the database.               |
| Log all CRUD operations through the backend with actor ID and affected record                        | Perform a sequence of create, update, and delete operations and verify each is recorded in the audit log with the correct actor, record reference, timestamp, and operation type.                          |

___

### 3.3. Threat Profile

#### 3.3.3 Threat Profile

| **Threat ID**   | **Threat Description**                                               | **Non Mitigated**  | **Partially Mitigated**   | **Fully Mitigated**   |
|-----------------|----------------------------------------------------------------------|:------------------:|:-------------------------:|:---------------------:|
| U1              | Forged/stolen JWT used to impersonate a legitimate user.             |                    |             X             |                       |
| U2              | Brute-force / credential stuffing against the login endpoint.        |                    |             X             |                       |
| U3              | Request body tampered in transit.                                    |                    |                           |           X           |
| U4              | Credentials or JWT intercepted via network sniffing.                 |                    |                           |           X           |
| U5              | Endpoint flooding causing backend Denial of Service.                 |                    |             X             |                       |
| U6              | Customer crafts requests to support/admin-only endpoints.            |                    |                           |           X           |
| B1              | Forged JWT accepted due to weak or missing validation.               |                    |                           |           X           |
| B2              | Auth0 `client_secret` exposed in source control.                     |                    |             X             |                       |
| B3              | Missing input sanitization allows data injection.                    |                    |                           |           X           |
| B4              | CORS misconfiguration abuses semi-protected endpoints.               |                    |                           |           X           |
| B5              | No audit log for sensitive operations.                               |                    |             X             |                       |
| B6              | Verbose error messages expose internal implementation details.       |                    |                           |           X           |
| B7              | No rate limiting on resource-intensive endpoints.                    |                    |                           |           X           |
| B8              | Missing or misconfigured `RoleGuard` on a route.                     |                    |                           |           X           |
| A1              | ROPC token request intercepted and credentials replayed.             |                    |                           |           X           |
| A2              | Auth0 tenant misconfiguration exploited by attacker.                 |                    |             X             |                       |
| A3              | client_id/secret tampered to redirect authentication to a rogue IdP. |                    |             X             |                       |
| A4              | ROPC payload exposed if TLS not enforced toward Auth0.               |                    |                           |           X           |
| A5              | Repeated failures trigger Auth0 lockout of legitimate users.         |                    |             X             |                       |
| A6              | Leaked `client_secret` allows direct token requests to Auth0.        |                    |             X             |                       |
| D1              | Overpermissive DB account impersonates higher-privilege role.        |                    |                           |           X           |
| D2              | SQL injection via unsanitized parameters.                            |                    |                           |           X           |
| D3              | Compromised backend modifies records outside its scope.              |                    |             X             |                       |
| D4              | No database audit log, actions untraceable.                          |                    |             X             |                       |
| D5              | Row-level controls missing, data cross-exposure.                     |                    |                           |           X           |
| D6              | Unbounded queries saturate database resources.                       |                    |                           |           X           |