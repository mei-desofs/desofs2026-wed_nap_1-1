package com.example.desofs.shared.mappers;

import com.example.desofs.domain.AuditLog;
import com.example.desofs.shared.dtos.AuditLogDTO;

/** Interface for mapping audit domain objects to DTOs. */
public interface IAuditMapper {
    AuditLogDTO toDTO(AuditLog auditLog);
}
