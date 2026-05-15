package com.example.desofs.shared.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for refund requests.
 * <p>
 * Represents the data structure used to transfer refund request information
 * between the service layer and the API layer. Contains all relevant fields
 * of a refund request, including identifiers, amounts, status and timestamps.
 */
public class RefundRequestDTO {
    private Long id;
    private Long orderId;
    private String userId;
    private BigDecimal amount;
    private String status;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public RefundRequestDTO() {}

    /**
     * Constructs a new RefundRequestDTO with all fields.
     *
     * @param id refund request identifier
     * @param orderId associated order identifier
     * @param userId identifier of the user who made the request
     * @param amount refund amount
     * @param status current status of the refund request
     * @param reason reason for the refund request
     * @param createdAt timestamp when the request was created
     * @param updatedAt timestamp when the request was last updated
     */
    public RefundRequestDTO(Long id, Long orderId, String userId, BigDecimal amount, String status, 
                            String reason, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.status = status;
        this.reason = reason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
