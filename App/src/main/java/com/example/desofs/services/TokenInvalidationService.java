package com.example.desofs.services;

import com.example.desofs.domain.UserTokenInvalidation;
import com.example.desofs.repositories.UserTokenInvalidationRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Manages server-side invalidation of OAuth2 access tokens.
 *
 * <p>OAuth2 access tokens issued by Auth0 are stateless JWTs that cannot be
 * revoked at the Identity Provider. To make privilege changes effective
 * immediately (instead of waiting for the token TTL to expire), this service
 * maintains a denylist keyed by Auth0 user id. The {@code TokenFreshnessFilter}
 * consults this denylist on every authenticated request and rejects tokens
 * whose {@code iat} claim is strictly before the recorded cutoff.</p>
 *
 * <p>This addresses OWASP ASVS V3.3 (Session Termination) for stateless
 * token-based sessions.</p>
 */
@Service
public class TokenInvalidationService implements ITokenInvalidationService {

    private static final Logger log = LoggerFactory.getLogger(TokenInvalidationService.class);

    private final UserTokenInvalidationRepository repository;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring-managed singleton repository injected via constructor")
    public TokenInvalidationService(UserTokenInvalidationRepository repository) {
        this.repository = repository;
    }

    /**
     * Records (or refreshes) a denylist entry for the given user with the
     * cutoff set to the current server instant.
     *
     * <p>Idempotent: calling it multiple times with the same user id simply
     * advances the cutoff forward.</p>
     *
     * @param userId Auth0 user identifier
     * @param reason short, non-sensitive justification (e.g.
     *               {@code ROLE_CHANGE}); stored verbatim for audit
     */
    @Override
    @Transactional
    public void invalidateTokensFor(String userId, String reason) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must be provided");
        }
        String safeReason = (reason == null || reason.isBlank()) ? "UNSPECIFIED" : reason;
        Instant now = Instant.now();
        UserTokenInvalidation entry = repository.findById(userId)
                .orElseGet(() -> new UserTokenInvalidation(userId, now, safeReason));
        entry.setInvalidatedAfter(now);
        entry.setReason(safeReason);
        entry.setUpdatedAt(now);
        repository.save(entry);
        log.info("Invalidated all tokens for user (reason={})", safeReason);
    }

    /**
     * Returns the current invalidation cutoff for the given user, if any.
     *
     * @param userId Auth0 user identifier
     * @return cutoff instant, or empty if the user has no denylist entry
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<Instant> getInvalidatedAfter(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        return repository.findById(userId).map(UserTokenInvalidation::getInvalidatedAfter);
    }

    /**
     * Tests whether a token with the given {@code iat} for the given user
     * has been invalidated by an administrative action.
     *
     * @param userId  Auth0 user identifier
     * @param tokenIssuedAt the {@code iat} claim of the token under check
     * @return {@code true} when the token must be rejected
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isTokenInvalidated(String userId, Instant tokenIssuedAt) {
        if (userId == null || tokenIssuedAt == null) {
            return false;
        }
        return repository.findById(userId)
                .map(UserTokenInvalidation::getInvalidatedAfter)
                .map(cutoff -> tokenIssuedAt.isBefore(cutoff))
                .orElse(false);
    }
}
