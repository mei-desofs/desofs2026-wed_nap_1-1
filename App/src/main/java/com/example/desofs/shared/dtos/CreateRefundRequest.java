package com.example.desofs.shared.dtos;

import java.math.BigDecimal;

public class CreateRefundRequest {
    private Long orderId;
    private Long userId;
    private BigDecimal amount;
    private String reason;

    public CreateRefundRequest() {}

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
