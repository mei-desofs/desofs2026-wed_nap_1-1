package com.example.desofs.shared.dtos;

/**
 * Request payload used to reject a refund request.
 */
public class RejectRefundRequest {
    private String reason;

    /**
     * Returns the rejection reason.
     *
     * @return reason text
     */
    public String getReason() {
        return reason;
    }

    /**
     * Sets the rejection reason.
     *
     * @param reason reason text
     */
    public void setReason(String reason) {
        this.reason = reason;
    }
}
