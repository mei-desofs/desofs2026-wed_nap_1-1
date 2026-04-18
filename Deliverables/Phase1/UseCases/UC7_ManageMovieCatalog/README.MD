# Use Case 7: Manage Movie Catalog

## 1. Description
### 1.1 Objective
This Use Case allows an Admin to manage the eMovieShop movie catalog.
It covers two operations: retrieving the full movie catalog and performing add, edit, or remove actions on individual movies.

### 1.2 Actors
* **Admin:** Primary actor responsible for maintaining the movie catalog.

### 1.3 Use/Abuse Case Diagram
This diagram illustrates the legitimate path for managing the movie catalog versus potential abuse scenarios, such as unprivileged users attempting to modify catalog data.

![Use and Abuse Cases - UC7](./diagrams/images/UseAbuseCase7.svg)

### 1.4 Pre-conditions
* The actor must be successfully authenticated.
* The actor must possess a valid JWT with the "Admin" role.

### 1.5 Post-conditions
* The movie catalog is successfully retrieved from the database, or the requested modification (add/edit/remove) is applied and confirmed.
* The result is returned to the actor in a structured JSON response.
* An audit log entry is created recording the catalog access or modification.

---

## 2. Interaction Flow & Architecture
As the system is a backend-only API, the interaction follows a direct request-response pattern between the client and the server.

### 2.1 Interaction Flow (API Level)

**Flow 1 — Retrieve Movie Catalog:**
1. **Request:** The Admin (via API Client) sends a `GET` request to `/movies` including the JWT in the Authorization header.
2. **Validation:** The `AuthMiddleware` verifies the JWT signature and the `RoleGuard` confirms the actor has Admin privileges.
3. **Business Logic:** The `MovieController` invokes `MovieService.getMovieCatalog()` to retrieve all movie records.
4. **Data Retrieval:** The `MovieRepository` executes `findAllMovies()` and returns the full movie data set.
5. **Response:** The system returns a `200 OK` status with the JSON array containing the movie catalog.

**Flow 2 — Add / Edit / Remove a Movie:**
1. **Request:** The Admin (via API Client) sends a `POST`, `PUT`, or `DELETE` request to `/movies/{id}` including the JWT in the Authorization header.
2. **Validation:** The `AuthMiddleware` verifies the JWT signature and the `RoleGuard` confirms the actor has Admin privileges.
3. **Business Logic:** The `MovieController` invokes `MovieService.validateAndApplyChanges()` to validate the payload and apply the requested modification.
4. **Data Mutation:** The `MovieRepository` executes `save/update/deleteMovie()` and confirms the operation.
5. **Response:** The system returns a `200 OK` (or `201 Created` / `204 No Content`) status with a JSON response indicating success or failure.

### 2.2 Sequence Diagram
This diagram shows the internal backend logic and the sequence of calls between the Controller, Service, and Repository, highlighting the enforcement of security rules at the service layer.

![Sequence Diagram - UC7](./Diagrams/images/SequenceDiagram7.svg)

---


## 3. Threat Analysis
Specific threats to the process of managing the movie catalog were evaluated using STRIDE and Attack Trees.

### 3.1 STRIDE Table
| Threat                                                              | Category                              | Mitigation Strategy                                                  |
|:--------------------------------------------------------------------|:--------------------------------------|:---------------------------------------------------------------------|
| Unauthenticated user attempts to access or modify the movie catalog | **Spoofing / Information Disclosure** | Mandatory JWT verification for all catalog endpoints.                |
| A Customer role user attempts to call an Admin-only endpoint        | **Elevation of Privilege**            | Server-side RBAC (Role-Based Access Control) enforcement.            |
| Attacker submits malformed or oversized payloads to corrupt catalog | **Tampering**                         | Input validation and schema enforcement at the service layer.        |
| Attacker floods the API with catalog requests to cause a DoS        | **Denial of Service**                 | Implementation of Rate Limiting middleware on the API.               |
| Movie catalog data is intercepted or tampered with during transit   | **Information Disclosure**            | Enforced use of TLS (HTTPS) for all API communications (ASVS 9.1.1). |

---

## 4. Security Requirements (ASVS Compliance)
Based on the ASVS checklist, the following requirements are strictly enforced for this UC:

* **ASVS V8.2.1 (Authorization):** All access control is enforced at the trusted backend service layer. The server validates the JWT role and permissions for every request to the catalog endpoints, ensuring that client-side controls cannot be bypassed.
* **ASVS V14.2.1 (Data Protection):** Sensitive data, such as session tokens (JWT), are never exposed in the URL or query string. Tokens are passed exclusively via secure HTTP headers (e.g., Authorization: Bearer).
* **ASVS V5.1.1 (Input Validation):** All input for add and edit operations is validated against a strict schema before reaching the repository layer, preventing injection of malformed data into the catalog.
* **ASVS V16.3.1 (Logging):** Security-relevant events, including successful and failed catalog modifications, are logged with sufficient metadata (timestamps, source IP, and actor IDs) to allow for forensic investigation.
* **ASVS V8.3.1 (Authorization):** Authorization decisions are made at the operation level (Controller/URI). The system ensures that only the Admin subject has explicit permission to invoke catalog mutation operations.

---

## 5. Secure Development Requirements
* **Code Review:** Any changes to the mutation logic in `MovieService` or access rules in `RoleGuard` require a security-focused peer review.
* **Automated Testing:** Unit and integration tests must cover scenarios of unauthorized access (e.g., a Customer role attempting to POST/PUT/DELETE a movie) and invalid input payloads to ensure RBAC and validation integrity.