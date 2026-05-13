package com.example.desofs.services;

import com.example.desofs.domain.entities.AuditLog;
import com.example.desofs.domain.entities.User;
import com.example.desofs.domain.repositories.AuditLogRepository;
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

    public AuditLog log(String action, String entityType, Long entityId, User user, 
                        AuditLog.UserRole userRole, String ipAddress, String userAgent, 
                        String details, Boolean success) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setUser(user);
        log.setUserRole(userRole);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        log.setDetails(details);
        log.setSuccess(success);
        return auditLogRepository.save(log);
    }

    public AuditLog logSuccess(String action, String entityType, Long entityId, User user, 
                               AuditLog.UserRole userRole, String ipAddress, String userAgent) {
        return log(action, entityType, entityId, user, userRole, ipAddress, userAgent, null, true);
    }

    public AuditLog logFailure(String action, String entityType, Long entityId, User user, 
                               AuditLog.UserRole userRole, String ipAddress, String userAgent, String reason) {
        return log(action, entityType, entityId, user, userRole, ipAddress, userAgent, reason, false);
    }
}
