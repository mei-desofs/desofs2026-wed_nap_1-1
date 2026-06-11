package com.example.desofs.domain;

/**
 * Enumeration of possible states for an {@link com.example.desofs.domain.Order}.
 *
 * <ul>
 *   <li>{@code PENDING} - order placed but not yet processed.</li>
 *   <li>{@code COMPLETED} - order has been fulfilled.</li>
 *   <li>{@code REFUNDED} - order has been refunded to the customer.</li>
 * </ul>
 */
public enum OrderStatus {

    /** Order placed but not yet processed. */
    PENDING,

    /** Order successfully completed/fulfilled. */
    COMPLETED,

    /** Order that has been refunded. */
    REFUNDED
}
