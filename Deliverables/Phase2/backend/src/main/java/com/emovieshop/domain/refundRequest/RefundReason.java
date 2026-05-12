package com.emovieshop.domain.refundRequest;

import java.util.Objects;

/**
 * Value object that represents the textual reason provided for a refund request.
 *
 * <p>The value is immutable, normalized, and validated to prevent blank or oversize content.</p>
 */
public class RefundReason {

	public static final int MIN_LENGTH = 3;
	public static final int MAX_LENGTH = 500;

	private final String value;

	/**
	 * Creates a refund reason value object.
	 *
	 * @param value refund reason text
	 * @throws NullPointerException if {@code value} is null
	 * @throws IllegalArgumentException if {@code value} is blank or outside accepted length bounds
	 */
	private RefundReason(String value) {
		String normalized = normalize(value);
		validate(normalized);
		this.value = normalized;
	}

	/**
	 * Creates a {@code RefundReason} from raw text.
	 *
	 * @param value refund reason text
	 * @return a validated {@code RefundReason}
	 * @throws NullPointerException if {@code value} is null
	 * @throws IllegalArgumentException if {@code value} is blank or outside accepted length bounds
	 */
	public static RefundReason of(String value) {
		return new RefundReason(value);
	}

	/**
	 * Returns the normalized reason text.
	 *
	 * @return refund reason
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Compares this refund reason with another object for value equality.
	 *
	 * @param o object to compare against
	 * @return true when both objects are {@code RefundReason} instances with the same value
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		RefundReason that = (RefundReason) o;
		return value.equals(that.value);
	}

	/**
	 * Returns a hash code consistent with {@link #equals(Object)}.
	 *
	 * @return hash code for the wrapped reason value
	 */
	@Override
	public int hashCode() {
		return value.hashCode();
	}

	/**
	 * Returns the wrapped reason text.
	 *
	 * @return refund reason value
	 */
	@Override
	public String toString() {
		return value;
	}

	private static String normalize(String rawValue) {
		return Objects.requireNonNull(rawValue, "Refund reason cannot be null").trim();
	}

	private static void validate(String value) {
		if (value.isBlank()) {
			throw new IllegalArgumentException("Refund reason cannot be blank");
		}
		if (value.length() < MIN_LENGTH) {
			throw new IllegalArgumentException("Refund reason must have at least " + MIN_LENGTH + " characters");
		}
		if (value.length() > MAX_LENGTH) {
			throw new IllegalArgumentException("Refund reason cannot exceed " + MAX_LENGTH + " characters");
		}
	}

}
