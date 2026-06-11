package com.example.desofs.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Server-side denylist record used to invalidate any access token issued
 * to a user before {@link #getInvalidatedAfter()}.
 *
 * <p>The application is a stateless OAuth2 Resource Server: access tokens
 * are JWTs that cannot be revoked at the Identity Provider. To make role
 * changes take effect immediately, the application keeps a small denylist
 * keyed by Auth0 user id and compares the JWT {@code iat} claim against
 * the stored cutoff timestamp.</p>
 */
@Entity
@Table(name = "user_token_invalidations")
public class UserTokenInvalidation {

    /** Auth0 user identifier (e.g. {@code auth0|abc123}). */
    @Id
    @Column(name = "auth0_user_id", length = 200, nullable = false)
    private String auth0UserId;

    /**
     * Cutoff timestamp. Any access token whose {@code iat} claim is strictly
     * before this instant must be rejected.
     */
    @Column(name = "invalidated_after", nullable = false)
    private Instant invalidatedAfter;

    /** Human-readable reason for the invalidation (audit/diagnostic only). */
    @Column(name = "reason", length = 100, nullable = false)
    private String reason;

    /** Last time this record was inserted or updated. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UserTokenInvalidation() {
    }

    public UserTokenInvalidation(String auth0UserId, Instant invalidatedAfter, String reason) {
        this.auth0UserId = auth0UserId;
        this.invalidatedAfter = invalidatedAfter;
        this.reason = reason;
        this.updatedAt = Instant.now();
    }

    public String getAuth0UserId() { return auth0UserId; }
    public void setAuth0UserId(String auth0UserId) { this.auth0UserId = auth0UserId; }

    public Instant getInvalidatedAfter() { return invalidatedAfter; }
    public void setInvalidatedAfter(Instant invalidatedAfter) { this.invalidatedAfter = invalidatedAfter; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
