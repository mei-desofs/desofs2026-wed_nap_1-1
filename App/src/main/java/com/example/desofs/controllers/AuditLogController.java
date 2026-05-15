package com.example.desofs.controllers;

import com.example.desofs.domain.AuditLog;
import com.example.desofs.domain.Role;
import com.example.desofs.security.RoleGuard;
import com.example.desofs.services.AuditLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
/**
 * REST controller exposing endpoints to access audit log entries.
 * <p>
 * All endpoints are restricted to users with the {@link Role#ADMIN} role.
 */
public class AuditLogController {

    /** Service responsible for managing audit log entries. */
    private final AuditLogService auditLogService;

    /** Guard that enforces role-based access checks. */
    private final RoleGuard roleGuard;

    /**
     * Creates a new controller backed by the provided service and role guard.
     *
     * @param auditLogService service used to retrieve audit log data
     * @param roleGuard guard used to enforce admin-only access
     */
    public AuditLogController(AuditLogService auditLogService, RoleGuard roleGuard) {
        this.auditLogService = auditLogService;
        this.roleGuard = roleGuard;
    }

    /**
     * Returns all audit log entries. Requires {@link Role#ADMIN}.
     *
     * @param jwt authenticated JWT principal
     * @return list of all {@link AuditLog} records
     */
    @GetMapping
    public List<AuditLog> list(@AuthenticationPrincipal Jwt jwt) {
        roleGuard.requireRole(jwt, Role.ADMIN);
        return auditLogService.listAll();
    }

    /**
     * Retrieves a single audit log by id. Requires {@link Role#ADMIN}.
     *
     * @param jwt authenticated JWT principal
     * @param id identifier of the audit log entry
     * @return {@link ResponseEntity} containing the audit log, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<AuditLog> get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        roleGuard.requireRole(jwt, Role.ADMIN);
        return ResponseEntity.notFound().build();
    }
}
