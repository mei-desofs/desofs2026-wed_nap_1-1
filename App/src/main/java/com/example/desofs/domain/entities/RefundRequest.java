package com.example.desofs.domain.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity that represents a refund request made for an order.
 *
 * <p>This entity stores the related order and user, the requested amount,
 * the current request status, the reason provided by the customer, and the
 * timestamps used to track the request lifecycle.</p>
 */
@Entity
@Table(name = "refund_requests")
public class RefundRequest {
    public static final int REASON_MAX_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private RefundStatus status; // PENDING, APPROVED, REJECTED, COMPLETED

    @Column(length = REASON_MAX_LENGTH)
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Creates a new refund request with the initial timestamps and a pending status.
     */
    public RefundRequest() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = RefundStatus.PENDING;
    }

    /**
     * Returns the unique identifier of this refund request.
     *
     * @return the refund request id
     */
    public Long getId() { return id; }

    /**
     * Returns the order associated with this refund request.
     *
     * @return the linked order
     */
    public Order getOrder() { return order; }

    /**
     * Sets the order associated with this refund request.
     *
     * @param order the order to associate
     */
    public void setOrder(Order order) { this.order = order; }

    /**
     * Returns the user who created the refund request.
     *
     * @return the requesting user
     */
    public User getUser() { return user; }

    /**
     * Sets the user who created the refund request.
     *
     * @param user the requesting user
     */
    public void setUser(User user) { this.user = user; }

    /**
     * Returns the amount requested for refund.
     *
     * @return the refund amount
     */
    public BigDecimal getAmount() { return amount; }

    /**
     * Sets the amount requested for refund.
     *
     * @param amount the refund amount
     */
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    /**
     * Returns the current status of this refund request.
     *
     * @return the refund request status
     */
    public RefundStatus getStatus() { return status; }

    /**
     * Updates the status of this refund request and refreshes the modification timestamp.
     *
     * @param status the new status
     */
    public void setStatus(RefundStatus status) { this.status = status; this.updatedAt = LocalDateTime.now(); }

    /**
     * Returns the reason provided for the refund request.
     *
     * @return the refund reason
     */
    public String getReason() { return reason; }

    /**
     * Sets the reason provided for the refund request.
     *
     * @param reason the refund reason
     */
    public void setReason(String reason) { this.reason = reason; }

    /**
     * Returns the timestamp when the request was created.
     *
     * @return the creation timestamp
     */
    public LocalDateTime getCreatedAt() { return createdAt; }

    /**
     * Returns the timestamp when the request was last updated.
     *
     * @return the last update timestamp
     */
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    /**
     * Represents the lifecycle states for a refund request.
     */
    public enum RefundStatus {
        PENDING, APPROVED, REJECTED, COMPLETED
    }
}
