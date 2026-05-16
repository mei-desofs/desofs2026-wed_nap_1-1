package com.example.desofs.services;

import com.example.desofs.domain.Role;
import com.example.desofs.shared.dtos.AuditLogDTO;

import java.util.List;
import java.util.Optional;

/**
 * Interface for the audit log service.
 */
public interface IAuditLogService {
    List<AuditLogDTO> listAll();

    Optional<AuditLogDTO> get(Long id);

    AuditLogDTO log(String actorId, String targetUserId, Role role, String operation);

    AuditLogDTO logRoleAssignment(String actorId, String targetUserId, Role role);

    AuditLogDTO logRoleRemoval(String actorId, String targetUserId, Role role);
}
