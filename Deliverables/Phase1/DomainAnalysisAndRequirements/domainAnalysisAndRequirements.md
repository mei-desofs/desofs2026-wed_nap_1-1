# Domain Analysis/Requirements

**eMovieShop** is a web application that provides a secure and user-friendly platform for customers to browse and purchase movies. Administrators are responsible for managing the product catalog, inventory, and user roles, while support staff handle refund requests and assist users with account-related issues. The platform ensures a seamless experience across multiple user roles while maintaining strict security and privacy standards for all operations.

## 1. Domain Model

![Domain Model](Diagrams/images/domainModel.svg)

This domain model applies Domain-Driven Design (DDD) principles, grouping core business logic into three main aggregates: `User`, `Order`, and `Movie`. Each aggregate encapsulates its own invariants and behaviors, while cross-cutting concerns like refunds and user roles are modeled as structural or logical relationships.

The `Order` aggregate is rooted in the `Order` entity, which contains one or more `OrderItem`s and tracks its own `OrderStatus` (`PENDING`, `COMPLETED`, `REFUNDED`). Each item refers to a specific `Movie` and has quantity and pricing as value objects.

To support refunds, each `Order` may have **zero or one** `RefundRequest` (cardinality `0..1`). This entity holds metadata such as the request reason, status (`REQUESTED`, `APPROVED`, `REJECTED`), and date. It is **not modeled as a separate aggregate**, but is instead embedded within `Order` to preserve transactional consistency.

The `Movie` aggregate defines `Movie` as its root, composed with value objects like `MovieTitle`, `MovieGenre`, `MoviePrice`, and `StockQuantity`. These represent the catalog and inventory that administrators manage.

The `User` aggregate captures identity and access, with roles (`ADMIN`, `SUPPORT`, `CUSTOMER`) expressed as a value object. Role-based access is enforced by application logic but conceptually represented by dashed logical associations in the model.

All associations use precise cardinality (`1`, `1*`, `*`, `**`, `0..1`) to reflect business rules (with `**` explicitly allowing zero or more), and solid lines denote composition or ownership. For example, `OrderItem` and `RefundRequest` are tightly coupled to `Order`, ensuring order history and refund data remain consistent and immutable after creation.

*While not shown as aggregates, external services like email notifications and static movies assets (e.g., images, trailers) are supported by third-party integrations described in the architecture section.*


## 2. Use Cases

| UC Number |       Description        |         Actor(s)         |
|:---------:|:------------------------:|:------------------------:|
|    UC1    | Login to the application | Customer, Support, Admin |
|    UC2    | Browse available movies  |         Customer         |
|    UC3    |      Purchase movie      |         Customer         |
|    UC4    |      Request refund      |         Customer         |
|    UC5    |  View requested refunds  |         Support          |
|    UC6    |  Handle refund request   |         Support          |
|    UC7    |   Manage movie catalog   |          Admin           |
|    UC8    |    Manage user roles     |          Admin           |


![Use Case Diagram](Diagrams/images/useCases.svg)

## 3. Secure Functional Requirements

*This section defines security constraints applied directly to business logic and user operations.*

**3.1 Business Logic & Anti-Abuse Rules**
* **Customers can only request refunds for completed orders within 14 days of purchase** 
Enforces a fair refund policy and mitigates abuse of the system.
* **Support can only handle refund requests associated with existing and valid orders** 
Ensures refund processing is based on consistent and traceable data.
* **Movie prices must be positive and not exceed €500** 
Validates catalog integrity and avoids pricing errors or exploits.
* **Users cannot purchase more than 10 movies in a single order** 
Adds a business rule to minimize abuse and reduce risk of fraudulent bulk buys.

**3.2 Access Control & Authentication Policies**
* **User accounts are temporarily locked after 5 failed login attempts** 
To prevent brute-force attacks and unauthorized access across all roles.
* **Admins are the only users allowed to create, edit, or remove movie catalog entries** 
Prevents unauthorized modifications to products and pricing.
* **Role-based access control (RBAC) is enforced across all operations** 
Ensures users only interact with features appropriate to their role (Customer, Support, Admin).

**3.3 Data & Communication Integrity**
* **All input data (e.g., usernames, movie titles, refund messages) is sanitized and validated** 
Protects against injection attacks such as XSS or SQLi.
* **Media files (e.g., images, trailers) are retrieved over HTTPS from a trusted CDN**
Protects users from man-in-the-middle attacks or spoofed content sources.


## 4. Functional Security Requirements

*This section defines the technical security features and protocols implemented to protect the system.*

**Core Security Controls**
* **Authentication:** All users (Customers, Support, Admins) must log in with valid credentials before accessing any features. Sessions are securely managed and expire after inactivity.
* **Authorization:** Role-based access control (RBAC) is enforced: Customers can browse, purchase, and request refunds; Support can only process refund requests; Admins manage the movie catalog and user roles.
* **Use of Encryption:** Sensitive data (passwords, session tokens) is encrypted in transit (HTTPS) and at rest. Passwords are hashed using a secure algorithm (e.g., bcrypt).
* **Input Validation:** All user input is validated and sanitized. This includes form submissions for login, purchases, refund requests, and movie management, preventing injection attacks.
* **Error Handling:** The system gracefully handles errors and does not expose stack traces or sensitive system info to users. Unauthorized actions trigger proper security messages (e.g., “Access Denied”).
* **System Testing & Maintenance:** The application undergoes regular security testing (e.g., static code analysis, dependency checks). Patch management is in place to address known vulnerabilities in third-party libraries or dependencies.
* **Secure Communications:** Email and CDN services are accessed over secure HTTPS connections. External services are invoked via authenticated or encrypted protocols to preserve integrity and confidentiality.

**Session & Token Security**
* **JWT Entropy and Signature Strength:** All session tokens (JWTs) are generated using secure libraries that ensure at least 64 bits of entropy. The system uses strong signing algorithms (RS256 or HS512) to protect token integrity and prevent forgery.
* **Secure Token Storage:** Although tokens were initially stored in localStorage, the system is being updated to store them in HttpOnly and Secure cookies to reduce exposure to XSS attacks.

#### 4.1 Password Policy
* **User Password Creation & Management:**
    * User-defined passwords must be at least 12 characters in length, with consecutive spaces normalized.
    * Passwords are accepted up to 64 characters.
    * All printable Unicode characters (including symbols and emojis) are permitted.
    * Users can change their passwords via the account management interface (requires current and new password).
    * A password strength meter is provided to assist users in creating secure credentials.
    * No composition rules (e.g., requiring uppercase or symbols) to allow flexibility and compatibility with password managers.
    * Password managers, browser autofill, and paste functionality are fully supported.
* **Initial Temporary Passwords:**
    * Securely generated using a cryptographically strong random number generator.
    * At least 8 alphanumeric characters in length.
    * Automatically expire within 24 hours if unused and enforced to be changed on first login.
* **Credential Storage:**
    * Securely stored using the bcrypt algorithm with a work factor of 12.
    * Unique 128-bit salt per password ensures each hash is unique.

#### 4.2 Session Management and Re-authentication
* **Role-based Expiration:**
    * Admins: Session expires after 1 hour or 10 minutes of inactivity.
    * Support staff: Session expires after 2 hours or 15 minutes of inactivity.
    * Customers: Session expires after 6 hours or 30 minutes of inactivity.
* **Monitoring & Security:**
    * The frontend monitors idle behavior using keyboard/mouse activity.
    * Critical operations (e.g., role assignment, refund approval) require recent authentication, verified via the `iat` claim in the token.
    * Session cookies use HttpOnly, Secure, SameSite=Lax, and the `__Host-` prefix to enhance scoping security.


## 5. Non-Functional Security Requirements

*This section defines the operational quality attributes, including performance, reliability, and resilience.*

* **Movie purchase and refund request operations must respond within 1.5 seconds on average** Ensures a smooth and responsive experience when customers place orders or request refunds, even during peak usage.
* **Movie catalog and order history endpoints must return valid responses in 97% of requests** Prioritizes availability of product browsing and past purchases for all users.
* **The storefront (home and movie list views) must load interactively within 3 seconds for most users** Maintains an efficient and user-friendly browsing experience across devices.
* **The platform must maintain at least 99.9% uptime monthly** Ensures consistent access to purchasing, refunds, and user login with minimal disruptions.
* **In the event of system failures, user actions must degrade gracefully (e.g., fallback messages or partial views)** Prevents exposing sensitive system behavior and improves resilience without sacrificing user trust.


## 6. Secure Development Requirements

*This section defines the SSDLC processes and standards followed by the development team.*

* **Apply secure coding best practices:** Code follows established guidelines for input validation, authentication, access control, and data handling, reducing the risk of common vulnerabilities.
* **Perform code reviews with a security focus:** All critical business logic (e.g., order creation, refund handling, role restrictions) is reviewed manually and/or with automated tools.
* **Adopt a security-conscious domain-driven design (DDD) approach:** Sensitive operations are scoped within the appropriate aggregates (Order, User, Movie) with clear ownership and validation rules.
* **Identify and document potential threats during design:** Common risks like unauthorized refunds, price tampering, or role escalation are evaluated using informal threat modeling techniques.
* **Follow a structured testing strategy with security in mind:** Unit, integration, and acceptance tests include scenarios for authentication, unauthorized access, and invalid inputs.
* **Use maintained and secure frameworks and dependencies:** Dependencies are kept up to date and monitored for known vulnerabilities using tools like npm audit or Snyk.
* **Security-sensitive changes require approval via pull requests with at least one reviewer:** Enforces peer validation of potentially risky logic and ensures adherence to secure development practices.
