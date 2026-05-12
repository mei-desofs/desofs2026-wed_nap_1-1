package com.emovieshop.domain.refundRequest;

/**
 * Represents the lifecycle status of a refund request.
 *
 * <p>A refund request is initially created with the {@code REQUESTED} status
 * and can later be handled by Support staff through approval or rejection.</p>
 *
 * <ul>
 *     <li>{@link #REQUESTED} - The refund request has been submitted and is awaiting review.</li>
 *     <li>{@link #APPROVED} - The refund request has been accepted and approved for processing.</li>
 *     <li>{@link #REJECTED} - The refund request has been denied.</li>
 * </ul>
 */
public enum RefundStatus {
    /**
     * Indicates that the refund request was submitted
     * and is awaiting Support review.
     */
    REQUESTED,

    /**
     * Indicates that the refund request was approved
     * and can proceed to payment reversal or reimbursement.
     */
    APPROVED,

    /**
     * Indicates that the refund request was rejected
     * and will not be processed.
     */
    REJECTED
}
