package com.example.desofs.security;

import com.emovieshop.domain.model.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * RoleGuard enforces role-based access control by verifying
 * that the JWT token contains the required role in its custom claims.
 *
 * Auth0 must be configured with an Action/Rule that adds roles
 * to the token under the configured namespace (e.g. "https://emovieshop.com/roles").
 */
@Component
public class RoleGuard {

    private final String rolesClaimNamespace;

    public RoleGuard(
            @Value("${emovieshop.auth0.roles-claim:https://emovieshop.com/roles}") String rolesClaimNamespace) {
        this.rolesClaimNamespace = rolesClaimNamespace;
    }

    /**
     * Checks that the JWT contains the specified role in its custom roles claim.
     *
     * @throws AccessDeniedException if the token doesn't carry the required role
     */
    public void requireRole(Jwt jwt, Role requiredRole) {
        List<String> roles = jwt.getClaimAsStringList(rolesClaimNamespace);

        if (roles == null || !roles.contains(requiredRole.name())) {
            throw new AccessDeniedException(
                    "Access denied. Required role: " + requiredRole.name());
        }
    }
}
