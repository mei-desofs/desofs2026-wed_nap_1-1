# Use Case 2: View Available Movies

## 1. Description
### 1.1 Objective
This Use Case allows any user to retrieve and view the list of available movies in the eMovieShop catalog. 
It provides customers with visibility into what movies are currently on offer so they can make purchasing decisions.

### 1.2 Actors
* **Customer:** Primary actor browsing the movie catalog.

### 1.3 Use/Abuse Case Diagram
This diagram illustrates the legitimate path for retrieving the m list versus potential abuse scenarios, such as unprivileged users attempting to access sensitive transaction data.

![Use and Abuse Cases - UC5](./Diagrams/images/UseAbuseCase5.svg)

### 1.4 Pre-conditions
* The actor must be successfully authenticated.
* The actor must possess a valid JWT with the "Support" or "Admin" role.

### 1.5 Post-conditions
* The list of movie refund requests is successfully retrieved from the database.
* The information is returned to the actor in a structured JSON response.
* An audit log entry is created recording the access to the refund list data.

---

## 2. Interaction Flow & Architecture
As the system is a backend-only API, the interaction follows a direct request-response pattern between the client and the server.

### 2.1 Interaction Flow (API Level)
1. **Request:** The Actor (via API Client) sends a `GET` request to `/api/refunds` including the JWT in the Authorization header.
2. **Validation:** The `AuthMiddleware` verifies the JWT signature and the `RoleGuard` confirms the actor has Support/Admin privileges.
3. **Business Logic:** The `RefundController` invokes the `RefundService` to retrieve all movie refund records from the repository.
4. **Data Retrieval:** The system queries the database for records associated with existing and valid movie orders.
5. **Response:** The system returns a `200 OK` status with the JSON array containing the refund requests details.

### 2.2 Sequence Diagram
This diagram shows the internal backend logic and the sequence of calls between the Controller, Service, and Repository, highlighting the enforcement of security rules at the service layer.

![Sequence Diagram - UC5](./Diagrams/images/SequenceDiagram5.svg)

---


## 3. Threat Analysis
Specific threats to the process of viewing refunds were evaluated using STRIDE and Attack Trees.

### 3.1 STRIDE Table
| Threat                                                          | Category                              | Mitigation Strategy                                                  |
|:----------------------------------------------------------------|:--------------------------------------|:---------------------------------------------------------------------|
| Unauthenticated user attempts to access the refund list         | **Spoofing / Information Disclosure** | Mandatory JWT verification for the endpoint.                         |
| A Customer role user attempts to call the Support-only endpoint | **Elevation of Privilege**            | Server-side RBAC (Role-Based Access Control) enforcement.            |
| Attacker floods the API with list requests to cause a DoS       | **Denial of Service**                 | Implementation of Rate Limiting middleware on the API.               |
| Sensitive refund data is intercepted during transit             | **Information Disclosure**            | Enforced use of TLS (HTTPS) for all API communications (ASVS 9.1.1). |

---

## 4. Security Requirements (ASVS Compliance)
Based on the ASVS checklist, the following requirements are strictly enforced for this UC:

* **ASVS V8.2.1 (Authorization):** All access control is enforced at the trusted backend service layer. The server validates the JWT role and permissions for every request to the refund list endpoint, ensuring that client-side controls cannot be bypassed.
* **ASVS V14.2.1 (Data Protection):** Sensitive data, such as session tokens (JWT) or movie IDs, are never exposed in the URL or query string. Tokens are passed exclusively via secure HTTP headers (e.g., Authorization: Bearer).
* **ASVS V2.3.2 (Business Logic):** The application enforces business logic limits and rules. Access to the global refund list is restricted to specific high-privilege roles (Support/Admin) as defined in the application's security documentation.
* **ASVS V16.3.1 (Logging):** Security-relevant events, including successful and failed access to sensitive data (like the refund list), are logged with sufficient metadata (timestamps, source IP, and actor IDs) to allow for forensic investigation.
* **ASVS V8.3.1 (Authorization):** Authorization decisions are made at the operation level (Controller/URI). The system ensures that the subject (Support/Admin) has the explicit permission to invoke the requested resource or perform the specific action.

---

## 5. Secure Development Requirements
* **Code Review:** Any changes to the retrieval logic in `RefundService` or access rules in `RoleGuard` require a security-focused peer review.
* **Automated Testing:** Unit and integration tests must cover scenarios of unauthorized access (e.g., a Customer role attempting to GET the refund list) to ensure RBAC integrity.