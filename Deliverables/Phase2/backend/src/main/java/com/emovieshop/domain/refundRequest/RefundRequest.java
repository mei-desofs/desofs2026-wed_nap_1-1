package com.emovieshop.domain.refundRequest;

import com.emovieshop.domain.model.Order;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain object that represents a refund request lifecycle.
 *
 * <p>A refund request starts in {@link RefundStatus#REQUESTED} and can be handled exactly once,
 * transitioning to {@link RefundStatus#APPROVED} or {@link RefundStatus#REJECTED}.</p>
 */
@Entity
@Table(name = "refund_requests")
public class RefundRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id", nullable = false, unique = true)
	private Order order;

	public static final int REASON_MAX_LENGTH = RefundReason.MAX_LENGTH;

	@Column(nullable = false, length = REASON_MAX_LENGTH)
	private String reason;

	@Column(name = "request_date", nullable = false, updatable = false)
	private LocalDateTime requestedAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RefundStatus status;

	protected RefundRequest() {
	}

	/**
	 * Creates a refund request.
	 *
	 * @param reason reason provided by the customer
	 * @param requestedAt date-time when the request was submitted
	 * @param status current lifecycle status
	 * @throws NullPointerException if any argument is null
	 */
	private RefundRequest(Order order, RefundReason reason, RefundDate requestedAt, RefundStatus status) {
		this.order = Objects.requireNonNull(order, "Order cannot be null");
		this.reason = Objects.requireNonNull(reason, "Refund reason cannot be null").getValue();
		this.requestedAt = Objects.requireNonNull(requestedAt, "Refund date cannot be null").getValue();
		this.status = Objects.requireNonNull(status, "Refund status cannot be null");
		this.order.setRefundRequest(this);
	}

	/**
	 * Creates a new refund request in {@link RefundStatus#REQUESTED} state.
	 *
	 * @param order order associated with the refund request
	 * @param reason reason provided by the customer
	 * @return a newly created refund request
	 * @throws NullPointerException if {@code order} or {@code reason} is null
	 */
	public static RefundRequest create(Order order, RefundReason reason) {
		return new RefundRequest(order, reason, RefundDate.now(), RefundStatus.REQUESTED);
	}

	/**
	 * Recreates an existing refund request from persisted data.
	 *
	 * @param order order associated with the refund request
	 * @param reason reason provided by the customer
	 * @param requestedAt date-time when the request was submitted
	 * @param status current lifecycle status
	 * @return a rehydrated refund request
	 * @throws NullPointerException if any argument is null
	 */
	public static RefundRequest rehydrate(Order order, RefundReason reason, RefundDate requestedAt, RefundStatus status) {
		return new RefundRequest(order, reason, requestedAt, status);
	}

	/**
	 * Approves this refund request.
	 *
	 * @throws IllegalStateException if the request is not in {@link RefundStatus#REQUESTED}
	 */
	public void approve() {
		ensureRequested();
		this.status = RefundStatus.APPROVED;
	}

	/**
	 * Rejects this refund request.
	 *
	 * @throws IllegalStateException if the request is not in {@link RefundStatus#REQUESTED}
	 */
	public void reject() {
		ensureRequested();
		this.status = RefundStatus.REJECTED;
	}

	/**
	 * Returns the persistence identifier.
	 *
	 * @return refund request id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * Returns the order associated with this refund request.
	 *
	 * @return linked order
	 */
	public Order getOrder() {
		return order;
	}

	/**
	 * Returns the customer-provided refund reason.
	 *
	 * @return refund reason
	 */
	public RefundReason getReason() {
		return RefundReason.of(reason);
	}

	/**
	 * Returns when the refund was requested.
	 *
	 * @return request timestamp value object
	 */
	public RefundDate getRequestedAt() {
		return RefundDate.of(requestedAt);
	}

	/**
	 * Returns current lifecycle status.
	 *
	 * @return refund status
	 */
	public RefundStatus getStatus() {
		return status;
	}

	/**
	 * Indicates whether this request is still awaiting handling.
	 *
	 * @return true if status is {@link RefundStatus#REQUESTED}
	 */
	public boolean isRequested() {
		return status == RefundStatus.REQUESTED;
	}

	private void ensureRequested() {
		if (!isRequested()) {
			throw new IllegalStateException("Refund request can only be handled from REQUESTED state");
		}
	}
}
