package com.example.desofs.repositories;

import com.example.desofs.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for persisting and retrieving {@link AuditLog} entries.
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
