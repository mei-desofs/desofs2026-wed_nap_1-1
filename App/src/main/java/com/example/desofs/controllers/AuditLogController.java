package com.example.desofs.controllers;

import com.example.desofs.domain.Role;
import com.example.desofs.security.IRoleGuard;
import com.example.desofs.services.IAuditLogService;
import com.example.desofs.shared.dtos.AuditLogDTO;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing endpoints to access audit log entries.
 * <p>
 * All endpoints are restricted to users with the {@link Role#ADMIN} role.
 */
@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    /** Service responsible for managing audit log entries. */
    private final IAuditLogService auditLogService;

    /** Guard that enforces role-based access checks. */
    private final IRoleGuard roleGuard;

    /**
     * Creates a new controller backed by the provided service and role guard.
     *
     * @param auditLogService service used to retrieve audit log data
     * @param roleGuard guard used to enforce admin-only access
     */
    public AuditLogController(IAuditLogService auditLogService, IRoleGuard roleGuard) {
        this.auditLogService = auditLogService;
        this.roleGuard = roleGuard;
    }

    /**
     * Returns all audit log entries. Requires {@link Role#ADMIN}.
     *
     * @param jwt authenticated JWT principal
    * @return list of all {@link AuditLogDTO} records
     */
    @GetMapping
    public List<AuditLogDTO> list(@AuthenticationPrincipal Jwt jwt) {
        roleGuard.requireRole(jwt, Role.ADMIN);
        return auditLogService.listAll();
    }

    /**
     * Retrieves a single audit log by id. Requires {@link Role#ADMIN}.
     *
     * @param jwt authenticated JWT principal
     * @param id identifier of the audit log entry
    * @return {@link ResponseEntity} containing the audit log DTO, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<AuditLogDTO> get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        roleGuard.requireRole(jwt, Role.ADMIN);
        return auditLogService.get(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
