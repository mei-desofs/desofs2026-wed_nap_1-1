package com.example.desofs.shared.dtos;

import java.util.List;

/**
 * Request payload used to create a new order.
 *
 * <p>Contains the identifier of the user placing the order and the list of
 * requested movie items.</p>
 */
public class CreateOrderRequest {
    /** Identifier of the user creating the order. */
    private Long userId;

    /** Items included in the order request. */
    private List<OrderItemRequest> items;

    /**
     * Creates an empty order creation request.
     */
    public CreateOrderRequest() {}

    /**
     * Returns the user identifier associated with this request.
     *
     * @return user identifier
     */
    public Long getUserId() { return userId; }

    /**
     * Sets the user identifier associated with this request.
     *
     * @param userId user identifier
     */
    public void setUserId(Long userId) { this.userId = userId; }

    /**
     * Returns the requested order items.
     *
     * @return list of order item requests
     */
    public List<OrderItemRequest> getItems() { return items; }

    /**
     * Sets the requested order items.
     *
     * @param items list of order item requests
     */
    public void setItems(List<OrderItemRequest> items) { this.items = items; }

    /**
     * Request payload for a single movie item in an order.
     */
    public static class OrderItemRequest {
        /** Identifier of the movie being ordered. */
        private Long movieId;

        /** Quantity requested for the movie. */
        private Integer quantity;

        /**
         * Creates an empty order item request.
         */
        public OrderItemRequest() {}

        /**
         * Returns the movie identifier.
         *
         * @return movie identifier
         */
        public Long getMovieId() { return movieId; }

        /**
         * Sets the movie identifier.
         *
         * @param movieId movie identifier
         */
        public void setMovieId(Long movieId) { this.movieId = movieId; }

        /**
         * Returns the requested quantity.
         *
         * @return requested quantity
         */
        public Integer getQuantity() { return quantity; }

        /**
         * Sets the requested quantity.
         *
         * @param quantity requested quantity
         */
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}
