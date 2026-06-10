package com.example.desofs.services;

import com.example.desofs.domain.Role;
import com.example.desofs.security.IAuth0ManagementClient;
import com.example.desofs.shared.dtos.UserDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;


/**
 * Service for user-related administration operations (UC8).
 *
 * <p>Delegates user listing and role mutations to the Auth0 Management API
 * and records each successful change in the audit log.</p>
 */
@Service
public class UserService implements IUserService {

    private final IAuth0ManagementClient auth0;
    private final IAuditLogService auditLogService;
    private final ITokenInvalidationService tokenInvalidationService;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring-managed singleton beans injected via constructor")
    public UserService(IAuth0ManagementClient auth0,
                       IAuditLogService auditLogService,
                       ITokenInvalidationService tokenInvalidationService) {
        this.auth0 = auth0;
        this.auditLogService = auditLogService;
        this.tokenInvalidationService = tokenInvalidationService;
    }

    @Override
    public List<UserDTO> getAllUsers() {
        return auth0.listUsers();
    }

    @Override
    public void assignRole(String actorId, String targetUserId, Role role) {
        guardSelfModification(actorId, targetUserId);
        Objects.requireNonNull(role, "role must not be null");
        auth0.assignRole(targetUserId, role);
        auditLogService.logRoleAssignment(actorId, targetUserId, role);
        invalidateUserSessions(targetUserId, "ROLE_ASSIGNED:" + role.name());
    }

    @Override
    public void removeRole(String actorId, String targetUserId, Role role) {
        guardSelfModification(actorId, targetUserId);
        Objects.requireNonNull(role, "role must not be null");
        auth0.removeRole(targetUserId, role);
        auditLogService.logRoleRemoval(actorId, targetUserId, role);
        invalidateUserSessions(targetUserId, "ROLE_REMOVED:" + role.name());
    }

    private void invalidateUserSessions(String targetUserId, String reason) {
        tokenInvalidationService.invalidateTokensFor(targetUserId, reason);
        auth0.invalidateSessions(targetUserId);
    }

    private static void guardSelfModification(String actorId, String targetUserId) {
        if (actorId == null || targetUserId == null
                || actorId.isBlank() || targetUserId.isBlank()) {
            throw new IllegalArgumentException("actorId and targetUserId are required");
        }
        if (actorId.equals(targetUserId)) {
            throw new IllegalArgumentException(
                    "Administrators cannot modify their own roles");
        }
    }
}
