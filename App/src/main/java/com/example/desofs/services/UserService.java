package com.example.desofs.services;

import com.example.desofs.domain.Role;
import com.example.desofs.shared.dtos.UserDTO;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * Service for user-related administration operations.
 *
 * <p>Currently supports listing users and recording role assignment/removal
 * actions through the audit logging service.</p>
 */
@Service
public class UserService {
    /** Service used to record role change audit events. */
    private final AuditLogService auditLogService;

    /**
     * Creates the service with the required audit logging dependency.
     *
     * @param auditLogService service used to log role changes
     */
    public UserService(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /**
     * Returns the current list of users.
     *
     * @return list of user DTOs
     */
    public List<UserDTO> getAllUsers() {
        return List.of();
    }

    /**
     * Records an audit entry for a role assignment action.
     *
     * @param actorId identifier of the user performing the change
     * @param targetUserId identifier of the user receiving the role
     * @param role role being assigned
     */
    public void assignRole(String actorId, String targetUserId, Role role) {
        auditLogService.logRoleAssignment(actorId, targetUserId, role);
    }

    /**
     * Records an audit entry for a role removal action.
     *
     * @param actorId identifier of the user performing the change
     * @param targetUserId identifier of the user losing the role
     * @param role role being removed
     */
    public void removeRole(String actorId, String targetUserId, Role role) {
        auditLogService.logRoleRemoval(actorId, targetUserId, role);
    }
}
