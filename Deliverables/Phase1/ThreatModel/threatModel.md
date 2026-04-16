# Threat Model

## 1. Threat Model Information

* **Application Name**: eMovies Shop

* **Application Version**: 1.0

* **Description**: eMovie Shop is an application for browsing, purchasing (excluding payment integration), managing, and refunding physical copies of movies in a videoclub store. Customers can view available movies, make purchase order and request refunds. Support users handle refund requests, while Admins oversee the movies catalog and manage user roles. The system implements secure authentication using JWTs and role-based access control (RBAC). It also integrates with external services such as:
  - Email providers (e.g., SendGrid) to send transactional emails for order confirmations and refund outcomes.

* **Document Owner**: DESOFT-2026-wed_nap_1

* **Participants**: Pedro Costa, Pedro Soares, Pedro Silva, Diogo Ribeiro, Miguel Cardoso

* **Reviewer**: Professor Paulo Baltarejo Sousa and Professor Nuno Pereira



## 2. External Dependencies

| ID | Description                                                                                                                                                                                                                                             |
|----|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1  | **MySQL** is used as the relational database for storing users, movies, orders, and refund requests. It runs on a Linux-based server or managed cloud service, and must be secured with restricted access, regular patching, and encrypted connections. |
| 2  | The **eMovie Shop Backend**, built in Node.js with Express, depends on the Node runtime and an ecosystem of external libraries. These must be actively maintained to avoid outdated packages introducing vulnerabilities.                               |
| 3  | **GitHub** is used for source control, issue tracking, and CI/CD. GitHub Actions automate builds, security checks (e.g., Semgrep, Dependabot), and testing, forming a critical dependency in the SDLC.                                                  |
| 4  | All communication with the system occurs over HTTPS, with API requests being made directly through tools such as Postman. Authentication is handled via Bearer tokens (JWT) included in the request headers. This setup assumes a correctly configured TLS-enabled server and appropriate network protections (e.g., firewall).                               |
| 5  | The system manages sensitive data such as **user roles, purchase history, and refund requests**. This makes **secure database access and audit logging** essential for accountability and protection against data tampering.                            |
| 6  | **Email Service** (e.g., SendGrid, Mailgun) is used to deliver transactional messages such as order confirmations and refund decisions. Communication must be authenticated and use encrypted channels (HTTPS or SMTP/TLS).                             |


## 3. Entry Points

| ID      | Name                 | Description                                                                                           | Trust Levels                                                |
| ------- |----------------------|-------------------------------------------------------------------------------------------------------| ----------------------------------------------------------- |
| 1       | HTTPS Port           | All access to eMovie Shop is served via HTTPS (TLS) through direct API requests.                      | (1) Guest, (2) Invalid Credentials, (3) Authenticated User  |
| *1.1*   | Authentication       | Customers, Support Staff, and Admins must authenticate to access protected features.                  | (1) Guest, (2) Invalid Credentials, (3) Authenticated Users |
| *1.1.1* | Login Function       | Submits credentials; returns a signed JWT on success. Token must be included in subsequent API calls. | (4) Customer, (5) Support, (6) Admin                        |
| 2       | API Endpoints        | All the system functionalities are exposed via REST API endpoints (e.g., /movies, /library)           | (4) Customer                                                |
| *2.1*   | Browse Movies        | Public endpoint to view movies. API call supports filtering and pagination.                           | (4) Customer                                                |
| *2.2*   | Purchase Movie       | Authenticated customers may trigger a purchase via a backend API call.                                | (4) Customer                                                |
| *2.3*   | View Library         | Customers can view previously purchased movies and refund-eligible items.                             | (4) Customer                                                |
| *2.4*   | Request Refund       | Triggers refund request for a specific OrderItem.                                                     | (4) Customer                                                |
| 3       | Support Tools        | API endpoints intended for support-level operations.                                                  | (5) Support                                                 |
| *3.1*   | View Refund Requests | Support users can list and approve/reject refunds.                                                    | (5) Support                                                 |
| 4       | Admin Console        | API endpoints reserved for administrative operations.                                                 | (6) Admin                                                   |
| *4.1*   | Manage Movie Catalog | Add/edit/remove movies and update prices or stock.                                                    | (6) Admin                                                   |
| *4.2*   | Manage User Roles    | Assign support/admin roles to registered users.                                                       | (6) Admin  

## 4. Exit Points

| ID    | Name                     | Description                                                                                           | Trust Levels                                               |
| ----- |--------------------------|-------------------------------------------------------------------------------------------------------| ---------------------------------------------------------- |
| 1     | HTTPS Responses          | All content is delivered via HTTPS, including API responses, HTML, JSON, and static assets.           | (1) Guest, (2) Invalid Credentials, (3) Authenticated User |
| *1.1* | Login Response           | After submitting credentials, the backend replies with a success message and signed JWT, or an error. | (1) Guest, (2) Invalid Credentials, (3) Authenticated User |
| *1.2* | Error Responses          | The system returns appropriate HTTP status codes and error messages (e.g., 400, 401, 403, 404).       | All trust levels                                           |
| 2     | Customer Responses       | Data shown to authenticated customers.                                                                | (4) Customer                                               |
| *2.1* | Movie List               | The customer sees a list of available movies, fetched via API.                                        | (4) Customer                                               |
| *2.2* | Purchase Confirmation    | After initiating a purchase, the server returns confirmation or validation errors.                    | (4) Customer                                               |
| *2.3* | Library / Owned Movies   | The customer can view a list of previously purchased movies.                                          | (4) Customer                                               |
| *2.4* | Refund Request Result    | After submitting a refund request, the response includes confirmation or rejection message.           | (4) Customer                                               |
| 3     | Support Responses        | Responses sent to support users managing refund flows.                                                | (5) Support                                                |
| *3.1* | Refund List API          | A list of pending refund requests is returned to the support dashboard.                               | (5) Support                                                |
| *3.2* | Refund Decision Result   | Confirmation/error response after approving or rejecting a refund request.                            | (5) Support                                                |
| 4     | Admin Responses          | Responses related to administrative operations.                                                       | (6) Admin                                                  |
| *4.1* | Catalog Update Result    | Confirmation/error after adding/editing/deleting a movie in the catalog.                              | (6) Admin                                                  |
| *4.2* | Role Assignment Feedback | Confirmation or errors when assigning roles to users.                                                 | (6) Admin                                                  |
| 5     | Email Notifications      | Transactional emails sent after successful purchases, refunds, or role changes                        | (4) Customer, (5) Support, (6) Admin                       |

## 5. Assets

## 6. Trust Levels

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
- On certain actions (e.g., completing an order, updating roles, processing refunds), the backend triggers outgoing emails via an external **Email Service** (e.g., SendGrid), shown as a one-way data flow.
- Red dashed lines indicate internal **trust boundaries**, while gray dashed lines represent **external communications** beyond system control.

This decomposition helps clarify integration points, potential exit paths, and responsibilities of each core component — which is especially useful for threat modeling and secure architecture analysis.