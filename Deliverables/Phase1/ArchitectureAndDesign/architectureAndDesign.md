## 2 Architecture/Design

### 2.1 Components

#### 2.1.1 High-Level

![High-Level Component Diagram](diagrams/images/HighLevelView.png)

This high-level diagram outlines the core containers of the eMovieShop system, divided across backend and infrastructure responsibilities.

- **User** interacts with the application via an **API Client**, accessing the **eMovieShop Backend**.

**Backend Container**:

- **eMovieShop Backend** is a secured REST API.
- It handles:
    - **JWT-based authentication** via middleware.
    - **Role-based authorization** (RBAC) using a `RoleGuard` helper.
    - Execution of core business logic for ordering, refunding, user role management, and movie catalog administration.
- The backend interacts with a **Database** for persistent state.

**Database**:

- A **Relational Database** stores all business entities, including:
    - Users, Movies, Orders, Refund Requests, and Roles.
- It supports queries from both the backend services and audit logging components.

#### 2.1.2 Backend

![Backend Logical View](diagrams/images/BackendLogicalView.png)

This diagram illustrates the internal architecture of the eMovieShop backend, designed with layered responsibilities and Domain-Driven Design (DDD).

- Incoming requests enter through the **Backend API**, carrying a `Bearer <JWT>` in the `Authorization` header.
- Requests are intercepted by the **Auth Middleware**, which verifies the JWT and blocks unauthorized access before reaching the controller layer.
- The **Controller** handles HTTP routing and delegates logic to the **Service Layer** using structured **DTOs**, which serve as a serialization and data-mapping layer.
- The **Service Layer** performs business operations such as movie purchases, refund processing, and role assignments.
    - It consults **Domain Models** through a **Model API**.
    - It accesses persistence through a **Repository API**.
    - It enforces role-based access control via a **RoleGuard**, ensuring actions are authorized.
- The **Domain Model** includes core aggregates like `User`, `Movie`, `Order`, and `RefundRequest`, encapsulating business rules and invariants.
- The **Repository** implements data persistence using domain entities, connected to a **MySQL Database**, abstracted behind repository interfaces.

The backend enforces security at multiple layers (middleware, services) and clearly separates concerns between API handling, 
domain logic, and data persistence.

### 2.2 Deployment

![Deployment View](diagrams/images/DeploymentView.png)

This deployment diagram represents the physical architecture of eMovieShop, deployed across three main servers and supported 
by external services.

- **Users** access the system via an HTTP client which:
    - Calls the **Backend Server API**, attaching a `Bearer <JWT>` token from `localStorage`.
- The **Backend Server** is responsible for:
    - **Validating JWT tokens** via middleware.
    - **Enforcing role-based access** using a `RoleGuard` component.
    - Executing core business logic like purchases, refunds, and role changes.
- The **Database Server** handles persistence for:
    - Users, roles, movies, orders, and refund requests.
    - Audit trails are recorded separately in the **Database Audit Logger**.

*Communication flows over secure HTTP/S between the http client, backend, and external services. Internal service-to-service 
communication (backend to database) uses TCP/IP.*

To align with secure deployment practices and the principle of the least privilege, each major server component is intended 
to run under its own dedicated low-privilege operating system account. This ensures compartmentalization and reduces the impact 
surface of a potential compromise:

- `emovieshop_backend`: Executes API logic with limited filesystem and network permissions.
- `emovieshop_db`: Dedicated database user with restricted access to only the required schema and operations.
- External services are accessed over HTTPS with API keys stored securely in environment variables, not in code or user accounts.

This operating model can be enforced via Docker container users or systemd service accounts.

### 2.3 Technology Stack

|   Component    | Technology Stack |
|:--------------:|:----------------:|
|    Backend     |       Java       |
|    Database    |      MySQL       |

### 2.4 Secure Design Patterns

| **Pattern**                     | **Adaptation in eMovieShop**                                                                                                                                                                                                                                |
|---------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Secure by Default**           | Unauthorized actions (e.g., unauthorized refunds or admin actions) are blocked by default using the `RoleGuard` check on the backend. Missing or invalid JWTs deny access automatically.                                                                    |
| **Layered Security**            | Security is applied at multiple levels: input is validated on both the frontend and backend; JWTs are verified at every request; and role-based access is enforced server-side. External services (CDN, email) are only invoked after backend verification. |
| **Minimal Access Rights**       | Each user role (Admin, Support, Customer) has only the permissions it needs. For example, only Support can approve refunds; Admins manage catalog and roles; Customers can only view/request their own orders.                                              |
| **Transparent Security Design** | The system does not rely on “security through obscurity.” Instead, it uses open, testable mechanisms like JWT, RBAC, and clearly defined API interfaces for enforcing access control.                                                                       |
| **Clean & Safe Code Practices** | Code is organized into layered components (DTO, Service, Controller, etc.), centralizes security logic (e.g., Auth Middleware), and uses best practices like clear naming, error handling, and input sanitation.                                            |
| **Session Binding and Entropy** | JWTs are signed using RS256/HS512 and generated with ≥64 bits of entropy. Tokens will no longer be stored in `localStorage`, but in secure, `HttpOnly` cookies to reduce XSS risk.                                                                          |

eMovieShop’s architecture integrates key secure design patterns across both backend and frontend. **Secure by Default** is 
enforced via the `Auth Middleware`, which blocks unauthenticated users before reaching controllers. **Layered Security** 
appears in both diagrams: React handles client-side form validation, while the backend enforces constraints through DTOs, 
services, and immutable domain value objects like `Email` or`MovieTitle`. **Least Privilege** is reflected in the `RoleGuard` 
service, restricting backend logic based on user roles, and in use cases tied to specific actors (e.g., only support can process refunds). 
The system embraces **Open Design** — JWT, RBAC, and authorization mechanisms are explicitly modeled in diagrams and thoroughly
documented. Lastly, **Coding Best Practices** are seen in the clear separation of concerns from API to domain in the backend 
and DDD patterns in the domain model.

### 2.5 Sequence Diagrams

**UC1:**

![Use Case 1 Sequence Diagram](../UseCases/UC1_Login/diagrams/images/SequenceDiagram1.svg)

**UC2:**

![Use Case 2 Sequence Diagram](../UseCases/UC2_ViewAvailableMovies/diagrams/images/SequenceDiagram2.svg)

**UC3:**

![Use Case 3 Sequence Diagram](../UseCases/UC3_Login/diagrams/images/SequenceDiagram3.svg)

**UC4:**

![Use Case 4 Sequence Diagram](../UseCases/UC4_Login/diagrams/images/SequenceDiagram4.svg)

**UC5:**
![Use Case 5 Sequence Diagram](../UseCases/UC5_ViewRequestRefunds/diagrams/images/SequenceDiagram5.svg)

**UC6:**
![Use Case 6 Sequence Diagram](../UseCases/UC6_HandleRefundRequest/diagrams/images/SequenceDiagram6.svg)

**UC7:**
![Use Case 7 Sequence Diagram](../UseCases/UC7_Login/diagrams/images/SequenceDiagram7.svg)

**UC8:**
![Use Case 8 Sequence Diagram](../UseCases/UC8_Login/diagrams/images/SequenceDiagram8.svg)
