# System Analysis — E-Movies Shop

## 1. Introduction
This document presents the analysis of the E-Movies Shop system, an online platform for browsing, purchasing, and managing movies. The analysis covers the domain, architecture, and security requirements, and also considers the backend process of generating and managing PDF invoices on the server after each purchase.

## 2. System Overview
E-Movies Shop is a web application that provides a secure and user-friendly environment for customers to search, view details, and purchase movies. The platform supports multiple user roles:
- **Administrators**: Manage the movie catalog, platform settings, and user roles.
- **Staff**: Provide customer support, handle refund requests, and assist users.
- **Buyers**: Browse, purchase movies, and access their purchase history.

The system ensures a seamless experience for all roles, maintaining strict security and privacy standards. An additional feature is the automatic generation of PDF invoices after each purchase, which involves backend operations for creating and managing files on the server.

## 3. System Architecture
- The architecture follows a layered approach, separating presentation, business logic, and data access.
- The backend is responsible for core operations, including user authentication, authorization, movie management, purchase processing, and PDF invoice generation.
- External services (e.g., email notifications) are integrated securely.

PDF invoice generation is handled by the backend, which creates user-specific directories and writes invoice files to the server, ensuring secure access and proper file management.

## 4. Domain Model
The domain model is structured around three main aggregates:
- **Purchase**: Rooted in the Purchase entity, containing one or more PurchaseItems, each linked to a Movie. Each purchase has a status (PENDING, COMPLETED, REFUNDED) and 

may include a PDF invoice (file path, creation date).
- **Movie**: Represents the catalog, with attributes like title, genre, price, and stock. Managed by administrators.
- **User**: Captures identity and access, with roles (ADMIN, STAFF, BUYER) as a value object. Role-based access is enforced throughout the application.

**Special Focus: Backend File Operations**
After a successful purchase, the backend creates a directory for the user if needed, generates a PDF invoice, and saves it on the server. This process includes secure directory creation, PDF file generation, error handling, and access control to ensure only authorized users can access their invoices.

## 5. Use Cases
| UC Number | Description                  | Actor(s)         |
|-----------|------------------------------|------------------|
| UC1       | Login to the application     | Buyer; Staff; Admin |
| UC2       | Browse available movies      | Buyer            |
| UC3       | Purchase movie               | Buyer            |
| UC4       | Download PDF invoice         | Buyer            |
| UC5       | Request refund               | Buyer            |
| UC6       | Handle refund request        | Staff            |
| UC7       | Manage movie catalog         | Admin            |
| UC8       | Manage user roles            | Admin            |

## 6. Security and Functional Requirements
- User accounts are locked after 5 failed login attempts (prevents brute-force attacks).
- Buyers can only request refunds for completed purchases within 14 days.
- Staff can only process refund requests for valid purchases.
- Only admins can create, edit, or remove movies.
- Movie prices must be positive and not exceed €500.
- Buyers cannot purchase more than 10 movies in a single order.
- All input data is sanitized and validated (prevents XSS, SQLi, etc.).
- Role-based access control (RBAC) enforced for all operations.
- PDF invoices are generated and made accessible to users after successful purchases, with file operations performed securely and with proper error handling.
- Media files are served over HTTPS from a trusted CDN.

## 7. Secure Development and OS Functionality
- Secure coding practices are applied to backend file operations, including directory creation and PDF file management.
- Code reviews and security testing include checks for file system vulnerabilities and error handling.
- Secure libraries are used for PDF generation and file management.
- Session tokens and sensitive data are securely stored and transmitted.

## 8. Conclusion
E-Movies Shop is designed with a strong focus on security, usability, and reliable backend operations. The system’s architecture and domain model ensure clear separation of concerns, and the secure handling of PDF invoice generation is addressed as part of the backend process. Future phases will further refine these mechanisms and expand on security testing and monitoring.
