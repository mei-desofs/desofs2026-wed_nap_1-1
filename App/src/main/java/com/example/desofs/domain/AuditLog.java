package com.example.desofs.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String actorId;       // jwt.getSubject() do Admin
    private String targetUserId;  // id do utilizador afetado
    private String role;
    private String operation;     // ASSIGN ou REMOVE
    private Instant timestamp;

    public static AuditLog of(String actorId, String targetUserId, Role role, String operation) {
        AuditLog log = new AuditLog();
        log.actorId = actorId;
        log.targetUserId = targetUserId;
        log.role = role.name();
        log.operation = operation;
        log.timestamp = Instant.now();
        return log;
    }

    public String getId() {
        return id;
    }

    public String getActorId() {
        return actorId;
    }

    public String getTargetUserId() {
        return targetUserId;
    }

    public String getRole() {
        return role;
    }

    public String getOperation() {
        return operation;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public void setTargetUserId(String targetUserId) {
        this.targetUserId = targetUserId;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
