package com.example.desofs.shared.mappers;

import com.example.desofs.domain.AuditLog;
import com.example.desofs.shared.dtos.AuditLogDTO;
import org.springframework.stereotype.Component;

/**
 * Mapper implementation for audit logs. Converts {@link AuditLog}
 * domain objects to {@link AuditLogDTO} transport objects.
 */
@Component
public class AuditMapper implements IAuditMapper {

	public AuditMapper() {
	}

	/**
	 * Maps a domain {@code AuditLog} to its corresponding DTO.
	 *
	 * @param auditLog domain entity to map
	 * @return mapped {@link AuditLogDTO}
	 */
	@Override
	public AuditLogDTO toDTO(AuditLog auditLog) {
		return new AuditLogDTO(
				auditLog.getId(),
				auditLog.getActorId(),
				auditLog.getTargetUserId(),
				auditLog.getRole(),
				auditLog.getOperation(),
				auditLog.getTimestamp()
		);
	}
}
