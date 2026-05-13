package com.example.desofs.services;

import com.example.desofs.domain.AuditLog;
import com.example.desofs.domain.Role;
import com.example.desofs.repositories.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public List<AuditLog> listAll() {
        return auditLogRepository.findAll();
    }

    public AuditLog log(String actorId, String targetUserId, Role role, String operation) {
        AuditLog auditLog = AuditLog.of(actorId, targetUserId, role, operation);
        return auditLogRepository.save(auditLog);
    }

    public AuditLog logRoleAssignment(String actorId, String targetUserId, Role role) {
        return log(actorId, targetUserId, role, "ASSIGN");
    }

    public AuditLog logRoleRemoval(String actorId, String targetUserId, Role role) {
        return log(actorId, targetUserId, role, "REMOVE");
    }
}
