# Use Case 6: Handle Refund Request

## Index
- [1. Description](#1-description)
	- [1.1 Objective](#11-objective)
	- [1.2 Actors](#12-actors)
	- [1.3 Use/Abuse Case Diagram](#13-useabuse-case-diagram)
	- [1.4 Pre-conditions](#14-pre-conditions)
	- [1.5 Post-conditions](#15-post-conditions)
- [2. Interaction Flow & Architecture](#2-interaction-flow--architecture)
	- [2.1 Interaction Flow (API Level)](#21-interaction-flow-api-level)
	- [2.2 Sequence Diagram](#22-sequence-diagram)
- [3. Threat Analysis](#3-threat-analysis)
	- [3.1 STRIDE Table](#31-stride-table)
- [4. Security Requirements (ASVS Compliance)](#4-security-requirements-asvs-compliance)
- [5. Secure Development Requirements](#5-secure-development-requirements)

## 1. Description
### 1.1 Objective
This Use Case allows users with the **Support** role to process pending movie refund requests by either approving or rejecting them. This ensures that refund processing is based on consistent and traceable data, maintaining the integrity of the platform's financial transactions.

### 1.2 Actors
* **Support Staff:** Primary actor responsible for evaluating and processing refund requests.

### 1.3 Use/Abuse Case Diagram
This diagram illustrates the legitimate path for handling a refund versus potential abuse scenarios, such as unauthorized users attempting to trigger or approve refunds.

![Use and Abuse Cases - UC6](./Diagrams/images/UseAbuseCase6.svg)

### 1.4 Pre-conditions
* A valid refund request must already exist in the system (submitted via UC4 and viewed via UC5).
* The actor must be successfully authenticated.
* The actor must possess a valid JWT with the `Support` role.

### 1.5 Post-conditions
* The status of the refund request is updated in the database (Approved or Rejected).
* The corresponding movie order status is updated accordingly.
* An audit log entry is created recording the decision, the actor's ID, and the timestamp.

---

## 2. Interaction Flow & Architecture
As the system is a backend-only API, the interaction follows a direct request-response pattern between the client and the server.

### 2.1 Interaction Flow (API Level)
1. **Request:** The Actor sends a `PATCH` request to `/api/refunds/{id}` with the decision (status) in the JSON body.
2. **Validation:** The `AuthMiddleware` verifies the JWT signature and the `RoleGuard` confirms the actor has Support privileges.
3. **Business Logic:** The `RefundController` calls the `RefundService`, which validates if the refund request is still in a "Pending" state and linked to a valid order.
4. **Transaction:** The system atomically updates the refund status and the movie order status in the database.
5. **Response:** The system returns a `200 OK` status with the updated refund details.

### 2.2 Sequence Diagram
This diagram shows the internal backend logic and the sequence of calls between the Controller, Service, and Repository, highlighting the enforcement of security rules at the service layer.

![Sequence Diagram - UC6](./Diagrams/images/SequenceDiagram6.svg)


---

## 3. Threat Analysis
Specific threats to the refund handling process were evaluated using STRIDE and Attack Trees.

### 3.1 STRIDE Table
| Threat | Category | Mitigation Strategy |
| :--- | :--- | :--- |
| Attacker impersonates Support staff to approve own refund | **Spoofing** | Mandatory JWT verification and server-side role check. |
| Malicious user modifies the refund ID in the request | **Tampering** | Validation of the refund ID against the database records before processing. |
| Support staff denies having approved a fraudulent refund | **Non-Repudiation** | Detailed audit logging (ASVS 16.3.2) with sufficient metadata to support forensic investigation and non-repudiation. |
| Customer tries to access the approval endpoint | **Elevation of Privilege** | RBAC enforced via `RoleGuard` at the controller level. |

---

## 4. Security Requirements (ASVS Compliance)
Based on the ASVS checklist, the following requirements are strictly enforced for this UC:

* **ASVS V2.3.2 (Validation and Business Logic):** Refund state transitions are only processed when the request is still in the expected pending state and linked to a valid order. The operation is rejected if the workflow is out of sequence or the submitted refund identifier does not match an existing record.
* **ASVS V8.2.1 and V8.3.1 (Authorization):** Function-level access to the refund handling endpoint is restricted to consumers with explicit permissions, and authorization is enforced at the trusted service layer rather than in client-controlled logic. Only users with the Support role may approve or reject refund requests in the current design.
* **ASVS V12.3.1 (Secure Communication):** All communication between the client and the backend API is protected with TLS so that refund data and authentication material are not exposed in transit.
* **ASVS V16.3.2 and V16.3.3 (Security Logging):** All refund decisions, rejected access attempts, and invalid state-transition attempts are logged with the requested resource, actor identity, decision outcome, timestamp, and source IP to support forensic investigation and non-repudiation.
* **ASVS V16.5.1 (Error Handling):** The API returns generic errors when access is denied or another unexpected failure occurs, without exposing sensitive internal details.

---

## 5. Secure Development Requirements
* **Code Review:** All changes to the `RefundService` logic require a security-focused peer review.
* **Automated Testing:** Unit tests must cover scenarios of unauthorized access (e.g., a Customer attempting to PATCH a refund) and invalid state transitions (e.g., approving an already rejected refund).