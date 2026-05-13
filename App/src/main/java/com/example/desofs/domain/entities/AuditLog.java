package com.example.desofs.domain.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String action; // REFUND_APPROVED, ORDER_COMPLETED, USER_CREATED, etc.
    private String entityType; // Order, RefundRequest, User, etc.
    private Long entityId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // User who performed the action

    @Enumerated(EnumType.STRING)
    private UserRole userRole; // CUSTOMER, SUPPORT, ADMIN

    private String ipAddress; // IPv4 or IPv6
    private String userAgent;
    private String details; // JSON with additional details
    private Boolean success;
    private LocalDateTime timestamp;

    public AuditLog() {
        this.timestamp = LocalDateTime.now();
        this.success = true;
    }

    public Long getId() { return id; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public UserRole getUserRole() { return userRole; }
    public void setUserRole(UserRole userRole) { this.userRole = userRole; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
    public LocalDateTime getTimestamp() { return timestamp; }

    public enum UserRole {
        CUSTOMER, SUPPORT, ADMIN
    }
}
