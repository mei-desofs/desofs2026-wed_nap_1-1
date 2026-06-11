package com.example.desofs.security;

import com.example.desofs.services.ITokenInvalidationService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

/**
 * Enforces server-side token freshness against the user invalidation
 * denylist.
 *
 * <p>OAuth2 access tokens are stateless JWTs and cannot be revoked at the
 * Identity Provider. Whenever an administrator changes a user's role
 * assignments, {@code TokenInvalidationService} records a cutoff timestamp
 * for that user. This filter compares the JWT {@code iat} claim against the
 * cutoff and rejects any token that was issued before it, producing an HTTP
 * 401 with an opaque body.</p>
 *
 * <p>The filter runs after {@code BearerTokenAuthenticationFilter}, so it
 * can read the authenticated {@link Jwt} from the security context. Public
 * endpoints (no JWT) are passed through unmodified.</p>
 *
 * <p>Addresses OWASP ASVS V3.3.1 / V3.3.4 (session termination upon
 * privilege change in stateless token-based sessions).</p>
 */
@Component
public class TokenFreshnessFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TokenFreshnessFilter.class);

    private final ITokenInvalidationService invalidationService;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring-managed singleton service injected via constructor")
    public TokenFreshnessFilter(ITokenInvalidationService invalidationService) {
        this.invalidationService = invalidationService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            chain.doFilter(request, response);
            return;
        }

        Jwt jwt = jwtAuth.getToken();
        String subject = jwt.getSubject();
        Instant issuedAt = jwt.getIssuedAt();

        if (subject == null || issuedAt == null) {
            chain.doFilter(request, response);
            return;
        }

        if (invalidationService.isTokenInvalidated(subject, issuedAt)) {
            log.info("Rejected request: token issued before invalidation cutoff for the subject");
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setHeader("WWW-Authenticate",
                    "Bearer error=\"invalid_token\", error_description=\"Token has been invalidated; please reauthenticate\"");
            response.getWriter().write(
                    "{\"error\":\"invalid_token\",\"message\":\"Token has been invalidated; please reauthenticate.\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}
