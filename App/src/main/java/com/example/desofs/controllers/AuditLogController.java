package com.example.desofs.controllers;

import com.example.desofs.services.AuditLogService;
import com.example.desofs.domain.AuditLog;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {
    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public List<AuditLog> list() {
        return auditLogService.listAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditLog> get(@PathVariable Long id) {
        // Implementation would require adding a method in the service
        return ResponseEntity.notFound().build();
    }
}
