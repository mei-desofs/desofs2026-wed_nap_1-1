package com.example.desofs.security;

import com.example.desofs.domain.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RoleGuardTest {

    private static final String ROLES_CLAIM = "https://emovieshop.com/roles";
    private RoleGuard roleGuard;

    @BeforeEach
    void setUp() {
        roleGuard = new RoleGuard(ROLES_CLAIM);
    }

    // ---- Success scenarios ----

    @Test
    @DisplayName("User with CUSTOMER role passes CUSTOMER check")
    void requireRole_customerHasCustomerRole_passes() {
        Jwt jwt = buildJwt(List.of("CUSTOMER"));
        assertDoesNotThrow(() -> roleGuard.requireRole(jwt, Role.CUSTOMER));
    }

    @Test
    @DisplayName("User with ADMIN role passes ADMIN check")
    void requireRole_adminHasAdminRole_passes() {
        Jwt jwt = buildJwt(List.of("ADMIN"));
        assertDoesNotThrow(() -> roleGuard.requireRole(jwt, Role.ADMIN));
    }

    @Test
    @DisplayName("User with SUPPORT role passes SUPPORT check")
    void requireRole_supportHasSupportRole_passes() {
        Jwt jwt = buildJwt(List.of("SUPPORT"));
        assertDoesNotThrow(() -> roleGuard.requireRole(jwt, Role.SUPPORT));
    }

    @Test
    @DisplayName("User with multiple roles passes if required role is present")
    void requireRole_multipleRoles_passes() {
        Jwt jwt = buildJwt(List.of("CUSTOMER", "ADMIN"));
        assertDoesNotThrow(() -> roleGuard.requireRole(jwt, Role.CUSTOMER));
        assertDoesNotThrow(() -> roleGuard.requireRole(jwt, Role.ADMIN));
    }

    // ---- Failure scenarios ----

    @Test
    @DisplayName("User without required role is denied")
    void requireRole_missingRole_throws() {
        Jwt jwt = buildJwt(List.of("CUSTOMER"));
        assertThrows(AccessDeniedException.class,
                () -> roleGuard.requireRole(jwt, Role.ADMIN));
    }

    @Test
    @DisplayName("User with no roles claim is denied")
    void requireRole_noRolesClaim_throws() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "auth0|user1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        assertThrows(AccessDeniedException.class,
                () -> roleGuard.requireRole(jwt, Role.CUSTOMER));
    }

    @Test
    @DisplayName("User with empty roles list is denied")
    void requireRole_emptyRolesList_throws() {
        Jwt jwt = buildJwt(List.of());
        assertThrows(AccessDeniedException.class,
                () -> roleGuard.requireRole(jwt, Role.CUSTOMER));
    }

    // ---- Privilege escalation tests ----

    @Test
    @DisplayName("CUSTOMER cannot access ADMIN endpoints")
    void requireRole_customerCannotBeAdmin_throws() {
        Jwt jwt = buildJwt(List.of("CUSTOMER"));
        assertThrows(AccessDeniedException.class,
                () -> roleGuard.requireRole(jwt, Role.ADMIN));
    }

    @Test
    @DisplayName("SUPPORT cannot access ADMIN endpoints")
    void requireRole_supportCannotBeAdmin_throws() {
        Jwt jwt = buildJwt(List.of("SUPPORT"));
        assertThrows(AccessDeniedException.class,
                () -> roleGuard.requireRole(jwt, Role.ADMIN));
    }

    @Test
    @DisplayName("ADMIN cannot access CUSTOMER-only endpoints")
    void requireRole_adminCannotBeCustomer_throws() {
        Jwt jwt = buildJwt(List.of("ADMIN"));
        assertThrows(AccessDeniedException.class,
                () -> roleGuard.requireRole(jwt, Role.CUSTOMER));
    }

    // ---- Case sensitivity and injection tests ----

    @Test
    @DisplayName("Role names are case-sensitive - lowercase fails")
    void requireRole_lowercaseRole_throws() {
        Jwt jwt = buildJwt(List.of("customer"));
        assertThrows(AccessDeniedException.class,
                () -> roleGuard.requireRole(jwt, Role.CUSTOMER));
    }

    @Test
    @DisplayName("Role with extra whitespace is rejected")
    void requireRole_roleWithWhitespace_throws() {
        Jwt jwt = buildJwt(List.of(" CUSTOMER "));
        assertThrows(AccessDeniedException.class,
                () -> roleGuard.requireRole(jwt, Role.CUSTOMER));
    }

    @Test
    @DisplayName("Role injection with special characters is rejected")
    void requireRole_roleInjection_throws() {
        Jwt jwt = buildJwt(List.of("CUSTOMER,ADMIN"));
        assertThrows(AccessDeniedException.class,
                () -> roleGuard.requireRole(jwt, Role.ADMIN));
    }

    // ---- Helper ----

    private Jwt buildJwt(List<String> roles) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "auth0|user1")
                .claim(ROLES_CLAIM, roles)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }
}

