package com.example.desofs.services;

import java.time.Instant;
import java.util.Optional;

/**
 * Contract for the server-side JWT denylist used to invalidate access
 * tokens issued before an administrative role change.
 */
public interface ITokenInvalidationService {

    /**
     * Records (or refreshes) the invalidation cutoff for the given user.
     *
     * @param userId Auth0 user identifier
     * @param reason short, non-sensitive justification for the invalidation
     */
    void invalidateTokensFor(String userId, String reason);

    /**
     * Returns the cutoff instant for the given user, when present.
     *
     * @param userId Auth0 user identifier
     * @return cutoff instant, or empty when no denylist entry exists
     */
    Optional<Instant> getInvalidatedAfter(String userId);

    /**
     * Determines whether a token with the given {@code iat} must be rejected.
     *
     * @param userId        Auth0 user identifier
     * @param tokenIssuedAt the {@code iat} claim of the token
     * @return {@code true} when the token has been invalidated
     */
    boolean isTokenInvalidated(String userId, Instant tokenIssuedAt);
}
