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