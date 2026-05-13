package com.example.desofs.shared.dtos;

import com.example.desofs.domain.RefundRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request payload used to create a refund request.
 *
 * <p>Contains the target order identifier, the refund amount, and the reason
 * supplied by the customer.</p>
 */
public class CreateRefundRequest {
    /** Identifier of the order to refund. */
    @NotNull(message = "Order ID is required")
    private Long orderId;

    /** Amount requested for the refund. */
    @NotNull(message = "Refund amount is required")
    @Positive(message = "Refund amount must be positive")
    private BigDecimal amount;

    /** Customer-provided reason for the refund request. */
    @NotBlank(message = "Refund reason is required")
    @Size(max = RefundRequest.REASON_MAX_LENGTH, message = "Refund reason must not exceed 500 characters")
    private String reason;

    /**
     * Creates an empty refund creation request.
     */
    public CreateRefundRequest() {}

    /**
     * Returns the order identifier.
     *
     * @return order identifier
     */
    public Long getOrderId() { return orderId; }

    /**
     * Sets the order identifier.
     *
     * @param orderId order identifier
     */
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    /**
     * Returns the refund amount.
     *
     * @return refund amount
     */
    public BigDecimal getAmount() { return amount; }

    /**
     * Sets the refund amount.
     *
     * @param amount refund amount
     */
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    /**
     * Returns the refund reason.
     *
     * @return refund reason
     */
    public String getReason() { return reason; }

    /**
     * Sets the refund reason.
     *
     * @param reason refund reason
     */
    public void setReason(String reason) { this.reason = reason; }
}
