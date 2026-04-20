# Use Case 1: Login to the application

## 1. Description
### 1.1 Objective

This Use Case allows any registered user (Customer, Support, or Admin) to authenticate
into the eMovie Shop system. It provides a secure mechanism for obtaining a JWT access
token, which is required to interact with all protected endpoints of the API.

### 1.2 Actors

* **Customer:** Primary actor authenticating to browse and purchase movies.
* **Support:** Actor authenticating to manage refund requests.
* **Admin:** Actor authenticating to manage the catalogue and assign user roles.


### 1.3 Use/Abuse Case Diagram

This diagram illustrates the legitimate authentication path, where credentials are delegated to Auth0 via the ROPC grant, versus potential abuse scenarios, including brute-force attacks against the login endpoint, credential interception via network sniffing, exposure of the Auth0 client secret and token replay attacks targeting the issued JWT.

![UseAbuseCases1.svg](resources/UseAbuseCases1.svg)

### 1.4 Pre-conditions
* The actor must have a registered account in the Auth0 tenant.
* The actor must provide valid credentials (username and password).
* The API must be reachable and the Auth0 tenant must be operational.

### 1.5 Post-conditions
* A valid JWT access token is issued by Auth0 and returned to the actor.
* The token contains the actor's role claims for downstream authorization.
* An audit log entry is created recording the authentication attempt (success or failure).

---

## 2. Interaction Flow & Architecture
As the system is a backend-only API, the interaction follows a direct request-response pattern between the client and the server, which delegates credential validation to Auth0 via the OAuth2 Resource Owner Password Credentials (ROPC) grant.

### 2.1 Interaction Flow (API Level)
1. **Request:** The Actor (via API Client) sends a `POST` request to `/api/auth/login` with a JSON body containing `username` and `password`.
2. **Delegation:** The `AuthController` invokes the `AuthService`, which forwards the credentials to Auth0 via the OAuth2 ROPC grant (`POST /oauth/token`).
3. **Validation:** Auth0 validates the credentials against the registered user store and, if valid, issues a signed JWT access token.
4. **Response:** The system returns a `200 OK` status with the JWT, or a `401 Unauthorized` if the credentials are invalid.

### 2.2 Sequence Diagram

This diagram shows the internal backend logic and the sequence of calls between the Controller, Service, and Repository in the UC1, highlighting the enforcement of security rules at the service layer.

![UseCase1.svg](resources/UseCase1.svg)

---

## 3. Threat Analysis

Specific threats to the process of viewing refunds were evaluated using STRIDE.


### 3.1 STRIDE Table

| Threat                                                                            | Category                               | Mitigation Strategy                                                                                          |
|:----------------------------------------------------------------------------------|:---------------------------------------|:-------------------------------------------------------------------------------------------------------------|
| Attacker submits forged or tampered credentials to impersonate a legitimate user  | **Spoofing**                           | Credentials are validated exclusively by Auth0; the backend never handles password verification directly.    |
| JWT is intercepted in transit and reused by an attacker                           | **Information Disclosure / Spoofing**  | Enforced use of TLS (HTTPS) for all API communications (ASVS 9.1.1).                                         |
| Attacker performs brute-force or credential stuffing against `/api/auth/login`    | **Denial of Service / Spoofing**       | Rate Limiting middleware on the login endpoint; Auth0 anomaly detection and attack protection.               |
| Attacker replays a captured valid JWT to gain unauthorized access                 | **Elevation of Privilege**             | Short token expiration (`expires_in`) and enforcement of token validation on every protected request.        |
| Malicious actor extracts `client_secret` from the backend to forge token requests | **Tampering / Elevation of Privilege** | `client_secret` stored exclusively as an environment variable; never hardcoded or exposed in source control. |



---

## 4. Security Requirements (ASVS Compliance)

Based on the ASVS checklist, the following requirements are strictly enforced for this UC:

* **ASVS V6.1.1 and V6.3.1 (Authentication):** User authentication is fully delegated to Auth0, a certified identity provider, and the login flow is protected against credential stuffing and brute-force attempts. The backend never stores or processes plaintext passwords.
* **ASVS V6.8.2 (Authentication with an Identity Provider):** Credentials are delegated to Auth0 and the integrity of the returned authentication assertion or JWT is validated before the application accepts it.
* **ASVS V12.2.1 and V12.3.1 (Secure Communication):** All communication between the API client, the backend, and Auth0 is enforced over TLS to prevent interception of credentials or tokens in transit.
* **ASVS V16.3.1 and V16.3.3 (Security Logging):** Authentication events (successful logins and failed attempts) and attempts to bypass authentication controls are logged with sufficient metadata (timestamps, source IP, and actor identity) to support forensic investigation and anomaly detection.

---

## 5. Secure Development Requirements
* **Code Review:** Any changes to the `AuthService` delegation logic or the `/api/auth/login` endpoint require a security-focused peer review, particularly regarding how credentials and the `client_secret` are handled.
* **Automated Testing:** Unit and integration tests must cover scenarios of failed authentication (invalid credentials, missing fields, expired tokens) to ensure the system responds correctly without leaking sensitive information in error messages.
* **Secret Management:** The Auth0 `client_id`, `client_secret`, and `audience` must be managed via environment variables and must never appear in source control. A secrets scanning tool should be integrated into the CI/CD pipeline.