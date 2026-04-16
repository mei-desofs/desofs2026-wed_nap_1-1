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

## 4. Exit Points

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