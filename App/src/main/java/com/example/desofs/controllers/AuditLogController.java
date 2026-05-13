package com.example.desofs.controllers;

import com.example.desofs.services.AuditLogService;
import com.example.desofs.domain.AuditLog;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
/**
 * REST controller exposing endpoints to access audit log entries.
 * <p>
 * Provides listing of audit logs and a placeholder for retrieving a single
 * audit log by id. The {@code get} endpoint currently returns 404 until the
 * corresponding service method is implemented.
 */
public class AuditLogController {
    
    /** Service responsible for managing audit log entries. */
    private final AuditLogService auditLogService;

    /**
     * Creates a new controller backed by the provided {@link AuditLogService}.
     *
     * @param auditLogService service used to retrieve audit log data
     */
    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /**
     * Returns all audit log entries.
     *
     * @return list of all {@link AuditLog} records
     */
    @GetMapping
    public List<AuditLog> list() {
        return auditLogService.listAll();
    }

    /**
     * Retrieves a single audit log by id.
     * <p>
     * Note: this endpoint currently returns 404 until the service exposes a
     * retrieval method for a single record.
     *
     * @param id identifier of the audit log entry
     * @return {@link ResponseEntity} containing the audit log when implemented,
     *         otherwise a 404 Not Found response
     */
    @GetMapping("/{id}")
    public ResponseEntity<AuditLog> get(@PathVariable Long id) {
        // Implementation would require adding a method in the service
        return ResponseEntity.notFound().build();
    }
}
