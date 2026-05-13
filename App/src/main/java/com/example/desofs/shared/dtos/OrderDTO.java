package com.example.desofs.shared.dtos;

import java.math.BigDecimal;
import java.util.List;

/**
 * Data Transfer Object representing an order in API responses.
 *
 * <p>Includes the order identity, customer information, line items, and
 * total amount.</p>
 */
public class OrderDTO {
    /** Order identifier. */
    private Long id;

    /** Identifier of the user who placed the order. */
    private Long userId;

    /** Email address associated with the user. */
    private String userEmail;

    /** Order line items. */
    private List<OrderItemDTO> items;

    /** Total order amount. */
    private BigDecimal total;

    /**
     * Creates an empty order DTO.
     */
    public OrderDTO() {}

    /**
     * Creates an order DTO with all fields populated.
     *
     * @param id order identifier
     * @param userId user identifier
     * @param userEmail user email address
     * @param items order items
     * @param total total amount
     */
    public OrderDTO(Long id, Long userId, String userEmail, List<OrderItemDTO> items, BigDecimal total) {
        this.id = id;
        this.userId = userId;
        this.userEmail = userEmail;
        this.items = items;
        this.total = total;
    }

    /**
     * Returns the order identifier.
     *
     * @return order identifier
     */
    public Long getId() { return id; }

    /**
     * Sets the order identifier.
     *
     * @param id order identifier
     */
    public void setId(Long id) { this.id = id; }

    /**
     * Returns the user identifier.
     *
     * @return user identifier
     */
    public Long getUserId() { return userId; }

    /**
     * Sets the user identifier.
     *
     * @param userId user identifier
     */
    public void setUserId(Long userId) { this.userId = userId; }

    /**
     * Returns the user email.
     *
     * @return user email
     */
    public String getUserEmail() { return userEmail; }

    /**
     * Sets the user email.
     *
     * @param userEmail user email address
     */
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    /**
     * Returns the order items.
     *
     * @return list of order item DTOs
     */
    public List<OrderItemDTO> getItems() { return items; }

    /**
     * Sets the order items.
     *
     * @param items list of order item DTOs
     */
    public void setItems(List<OrderItemDTO> items) { this.items = items; }

    /**
     * Returns the total order amount.
     *
     * @return total amount
     */
    public BigDecimal getTotal() { return total; }

    /**
     * Sets the total order amount.
     *
     * @param total total amount
     */
    public void setTotal(BigDecimal total) { this.total = total; }
}
