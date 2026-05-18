package com.example.desofs.services;

import com.example.desofs.domain.AuditLog;
import com.example.desofs.domain.Role;
import com.example.desofs.repositories.AuditLogRepository;
import com.example.desofs.shared.dtos.AuditLogDTO;
import com.example.desofs.shared.mappers.IAuditMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service responsible for persisting audit log entries.
 * <p>
 * Provides helper methods for logging role-related operations performed by one
 * user against another user.
 */
@Service
public class AuditLogService implements IAuditLogService {
    /** Repository used to persist audit log entries. */
    private final AuditLogRepository auditLogRepository;
    private final IAuditMapper auditMapper;

    /**
     * Creates a new audit log service.
     *
     * @param auditLogRepository repository used to store audit logs
     */
    public AuditLogService(AuditLogRepository auditLogRepository, IAuditMapper auditMapper) {
        this.auditLogRepository = auditLogRepository;
        this.auditMapper = auditMapper;
    }

    /**
     * Returns all persisted audit log entries.
     *
     * @return list of audit log records
     */
    public List<AuditLogDTO> listAll() {
        return auditLogRepository.findAll().stream()
            .map(auditMapper::toDTO)
            .toList();
    }

    /**
     * Retrieves a single audit log entry by id.
     *
     * @param id audit log identifier
     * @return optional audit log DTO
     */
    public Optional<AuditLogDTO> get(Long id) {
        return auditLogRepository.findById(id).map(auditMapper::toDTO);
    }

    /**
     * Persists a new audit log entry.
     *
     * @param actorId identifier of the user performing the action
     * @param targetUserId identifier of the user affected by the action
     * @param role role being assigned or removed
     * @param operation audit operation name, such as {@code ASSIGN} or {@code REMOVE}
     * @return persisted audit log entry
     */
    public AuditLogDTO log(String actorId, String targetUserId, Role role, String operation) {
        AuditLog auditLog = AuditLog.of(actorId, targetUserId, role, operation);
        AuditLog saved = auditLogRepository.save(auditLog);
        return auditMapper.toDTO(saved);
    }

    /**
     * Logs a role assignment operation.
     *
     * @param actorId identifier of the user performing the assignment
     * @param targetUserId identifier of the user receiving the role
     * @param role role that was assigned
     * @return persisted audit log entry
     */
    public AuditLogDTO logRoleAssignment(String actorId, String targetUserId, Role role) {
        return log(actorId, targetUserId, role, "ASSIGN");
    }

    /**
     * Logs a role removal operation.
     *
     * @param actorId identifier of the user performing the removal
     * @param targetUserId identifier of the user losing the role
     * @param role role that was removed
     * @return persisted audit log entry
     */
    public AuditLogDTO logRoleRemoval(String actorId, String targetUserId, Role role) {
        return log(actorId, targetUserId, role, "REMOVE");
    }
}
