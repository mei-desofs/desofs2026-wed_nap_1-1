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
|    UC9    |                          |                          |


![Use Case Diagram](Diagrams/images/useCases.svg)

## 3. 