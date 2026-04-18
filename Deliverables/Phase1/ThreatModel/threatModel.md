# Threat Model

## 1. Threat Model Information

* **Application Name**: eMovie Shop

* **Application Version**: 1.0

* **Description**: eMovie Shop is an application for browsing, purchasing (excluding payment integration), managing, and refunding physical copies of movies in a videoclub store. Customers can view available movies, make purchase order and request refunds. Support users handle refund requests, while Admins oversee the movies catalog and manage user roles. Authentication and authorization are handled through an external identity provider (Auth0), which issues JWTs used to secure API requests and enforce role-based access control (RBAC). The backend validates these tokens to ensure proper access control across all operations. The system relies on HTTPS for secure communication and assumes a properly configured TLS-enabled server and network protections.

* **Document Owner**: DESOFT-2026-wed_nap_1

* **Participants**: Pedro Costa, Pedro Soares, Pedro Silva, Diogo Ribeiro, Miguel Cardoso

* **Reviewer**: Professor Paulo Baltarejo Sousa and Professor Nuno Pereira



## 2. External Dependencies

| ID | Description                                                                                                                                                                                                                                                                                                                     |
|----|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1  | **MySQL** is used as the relational database for storing users, movies, orders, and refund requests. It runs on a Linux-based server or managed cloud service, and must be secured with restricted access, regular patching, and encrypted connections.                                                                         |
| 2  | The **eMovie Shop Backend**, built in Node.js with Express, depends on the Node runtime and an ecosystem of external libraries. These must be actively maintained to avoid outdated packages introducing vulnerabilities.                                                                                                       |
| 3  | **GitHub** is used for source control, issue tracking, and CI/CD. GitHub Actions automate builds, security checks (e.g., Semgrep, Dependabot), and testing, forming a critical dependency in the SDLC.                                                                                                                          |
| 4  | All communication with the system occurs over HTTPS, with API requests being made directly through tools such as Postman. Authentication is handled via Bearer tokens (JWT) included in the request headers. This setup assumes a correctly configured TLS-enabled server and appropriate network protections (e.g., firewall). |
| 5  | The system manages sensitive data such as **user roles, purchase history, and refund requests**. This makes **secure database access and audit logging** essential for accountability and protection against data tampering.                                                                                                    |
| 6  | **Auth0** is used as an external Identity Provider for user authentication, and JWT issuance. The system depends on Auth0 for secure token generation and validation (e.g., via JWKS). Proper configuration of roles, claims, and token verification is required to prevent authentication and authorization flaws.             |

## 3. Entry Points

| ID       | Name                  | Description                                                                                                         | Trust Levels                                                  |
|----------|-----------------------|---------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------|
| 1        | HTTPS Port            | All access to eMovie Shop is served via HTTPS (TLS) through direct API requests.                                    | (1) Guest, (2) Invalid Credentials, (3) Authenticated User    |
| *1.1*    | Authentication        | Users authenticate via an external Identity Provider (Auth0) to access protected features.                          | (1) Guest, (2) Invalid Credentials, (3) Authenticated Users   |
| *1.1.1*  | Login Function        | Users submit credentials to Auth0 and receive a signed JWT on success. This token must be included in API calls.    | (4) Customer, (5) Support, (6) Admin                          |
| 2        | API Endpoints         | All the system functionalities are exposed via REST API endpoints (e.g., /movies, /library)                         | (4) Customer                                                  |
| *2.1*    | Browse Movies         | Public endpoint to view movies. API call supports filtering and pagination.                                         | (4) Customer                                                  |
| *2.2*    | Purchase Movie        | Authenticated customers may trigger a purchase via a backend API call.                                              | (4) Customer                                                  |
| *2.3*    | View Library          | Customers can view previously purchased movies and refund-eligible items.                                           | (4) Customer                                                  |
| *2.4*    | Request Refund        | Triggers refund request for a specific OrderItem.                                                                   | (4) Customer                                                  |
| 3        | Support Tools         | API endpoints intended for support-level operations.                                                                | (5) Support                                                   |
| *3.1*    | View Refund Requests  | Support users can list and approve/reject refunds.                                                                  | (5) Support                                                   |
| 4        | Admin Console         | API endpoints reserved for administrative operations.                                                               | (6) Admin                                                     |
| *4.1*    | Manage Movie Catalog  | Add/edit/remove movies and update prices or stock.                                                                  | (6) Admin                                                     |
| *4.2*    | Manage User Roles     | Assign support/admin roles to registered users.                                                                     | (6) Admin                                                     |

## 4. Exit Points

| ID      | Name                         | Description                                                                                                      | Trust Levels                                                 |
|---------|------------------------------|------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------|
| 1       | HTTPS Responses              | All content is delivered via HTTPS, including API responses, HTML, JSON, and static assets.                      | (1) Guest, (2) Invalid Credentials, (3) Authenticated User   |
| *1.1*   | Login Response               | Authentication is handled by Auth0, which returns a signed JWT or an error. The backend only validates tokens.   | (1) Guest, (2) Invalid Credentials, (3) Authenticated User   |
| *1.2*   | Error Responses              | The system returns appropriate HTTP status codes and error messages (e.g., 400, 401, 403, 404).                  | All trust levels                                             |
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

## 6. Trust Levels

| ID       | Name                         | Description                                                                                                              |
|----------|------------------------------|--------------------------------------------------------------------------------------------------------------------------|
| **1**    | Guest                        | An unauthenticated actor interacting with the eMovie Shop without logging in. Limited to publicly accessible endpoints.  |
| **2**    | Invalid Credentials          | Actor that failed authentication via Auth0 and does not possess a valid JWT.                                             |
| **3**    | Authenticated User           | A user authenticated via Auth0 with a valid JWT. Includes all roles: Customer, Support, or Admin.                        |
| **4**    | Customer                     | A user with the `CUSTOMER` role. Can browse movies, make purchases, and request refunds.                                 |
| **5**    | Support                      | A user with the `SUPPORT` role. Can view, approve, or reject refund requests.                                            |
| **6**    | Admin                        | A user with the `ADMIN` role. Manages movies, stock, discounts, and assigns user roles.                                  |
| **7**    | System Administrator         | Person responsible for infrastructure, deployment, system monitoring, and security configuration.                        |
| **8**    | Database Administrator       | Full access to the MySQL database. Can create/modify tables, manage credentials, and ensure data integrity.              |
| **9**    | Database Read User           | A user or service with read-only access to the database (e.g., analytics or report generation).                          |
| **10**   | Database Read & Write User   | A user or service with privileges to query and modify records, but not manage the database schema.                       |

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
- Red dashed lines indicate internal **trust boundaries**, while gray dashed lines represent **external communications** beyond system control.

This decomposition helps clarify integration points, potential exit paths, and responsibilities of each core component — which is especially useful for threat modeling and secure architecture analysis.