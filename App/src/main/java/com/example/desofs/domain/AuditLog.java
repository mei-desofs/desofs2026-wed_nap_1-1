package com.example.desofs.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Audit log entry for role assignment and removal operations.
 * <p>
 * Stores the Auth0 actor id, the target user id, the role involved, and the
 * operation performed.
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog {
    /** Unique identifier of the audit log entry. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Auth0 id of the user who performed the action. */
    @Column(name = "actor_id", nullable = false)
    private String actorId;

    /** Auth0 id of the user affected by the action. */
    @Column(name = "target_user_id", nullable = false)
    private String targetUserId;

    /** Role assigned to or removed from the target user. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /** Operation performed, such as ASSIGN or REMOVE. */
    @Column(nullable = false)
    private String operation;

    /** Timestamp when the audit log entry was created. */
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    /**
     * Creates a new audit log entry with the creation timestamp initialized.
     */
    public AuditLog() {
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Creates a new audit log entry.
     *
     * @param actorId Auth0 user id of the actor performing the operation
     * @param targetUserId Auth0 user id of the affected user
     * @param role role assigned or removed
     * @param operation operation name, typically {@code ASSIGN} or {@code REMOVE}
     * @return initialized audit log entry
     */
    public static AuditLog of(String actorId, String targetUserId, Role role, String operation) {
        AuditLog auditLog = new AuditLog();
        auditLog.setActorId(actorId);
        auditLog.setTargetUserId(targetUserId);
        auditLog.setRole(role);
        auditLog.setOperation(operation);
        return auditLog;
    }

    /**
     * Returns the audit log identifier.
     *
     * @return the audit log id
     */
    public Long getId() { return id; }

    /**
     * Returns the actor Auth0 id.
     *
     * @return actor Auth0 id
     */
    public String getActorId() { return actorId; }

    /**
     * Sets the actor Auth0 id.
     *
     * @param actorId actor Auth0 id
     */
    public void setActorId(String actorId) { this.actorId = actorId; }

    /**
     * Returns the target user Auth0 id.
     *
     * @return target user Auth0 id
     */
    public String getTargetUserId() { return targetUserId; }

    /**
     * Sets the target user Auth0 id.
     *
     * @param targetUserId target user Auth0 id
     */
    public void setTargetUserId(String targetUserId) { this.targetUserId = targetUserId; }

    /**
     * Returns the role involved in the operation.
     *
     * @return audit log role
     */
    public Role getRole() { return role; }

    /**
     * Sets the role involved in the operation.
     *
     * @param role role involved in the audit log entry
     */
    public void setRole(Role role) { this.role = role; }

    /**
     * Returns the operation name.
     *
     * @return operation name
     */
    public String getOperation() { return operation; }

    /**
     * Sets the operation name.
     *
     * @param operation operation name
     */
    public void setOperation(String operation) { this.operation = operation; }

    /**
     * Returns the timestamp when the entry was created.
     *
     * @return creation timestamp
     */
    public LocalDateTime getTimestamp() { return timestamp; }
}
