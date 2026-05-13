package com.example.desofs.domain;

/**
 * Enumeration of application roles used for authorization checks.
 *
 * <p>Roles are used by the security layer and business logic to control
 * access to protected endpoints and operations.</p>
 */
public enum Role {
    /** Customer role for end users who place orders and request refunds. */
    CUSTOMER,
    /** Support role for staff who handle refund requests. */
    SUPPORT,
    /** Admin role for catalog and role management operations. */
    ADMIN
}
