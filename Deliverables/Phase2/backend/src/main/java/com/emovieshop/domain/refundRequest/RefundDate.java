package com.emovieshop.domain.refundRequest;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Value object that represents when a refund request was created.
 *
 * <p>The value is immutable and cannot be null.</p>
 */
public class RefundDate {

	private final LocalDateTime value;

	/**
	 * Creates a refund date value object.
	 *
	 * @param value date-time associated with the refund request
	 * @throws NullPointerException if {@code value} is null
	 */
	private RefundDate(LocalDateTime value) {
		this.value = Objects.requireNonNull(value, "Refund date cannot be null");
	}

	/**
	 * Creates a {@code RefundDate} with the current system date and time.
	 *
	 * @return a new {@code RefundDate} instance with {@link LocalDateTime#now()}
	 */
	public static RefundDate now() {
		return new RefundDate(LocalDateTime.now());
	}

	/**
	 * Creates a {@code RefundDate} from a provided timestamp.
	 *
	 * @param value the date-time value for the refund request
	 * @return a new {@code RefundDate} instance
	 * @throws NullPointerException if {@code value} is null
	 */
	public static RefundDate of(LocalDateTime value) {
		return new RefundDate(value);
	}

	/**
	 * Returns the underlying date-time value.
	 *
	 * @return refund request creation timestamp
	 */
	public LocalDateTime getValue() {
		return value;
	}

	/**
	 * Compares this refund date with another object for value equality.
	 *
	 * @param o object to compare against
	 * @return true when both objects are {@code RefundDate} instances with the same date-time value
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		RefundDate that = (RefundDate) o;
		return value.equals(that.value);
	}

	/**
	 * Returns a hash code consistent with {@link #equals(Object)}.
	 *
	 * @return hash code for the wrapped date-time value
	 */
	@Override
	public int hashCode() {
		return value.hashCode();
	}

	/**
	 * Returns a string representation of the wrapped date-time value.
	 *
	 * @return ISO-8601 string representation of the refund date-time
	 */
	@Override
	public String toString() {
		return value.toString();
	}
}
