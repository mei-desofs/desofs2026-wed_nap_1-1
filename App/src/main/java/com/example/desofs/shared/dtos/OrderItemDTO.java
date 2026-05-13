package com.example.desofs.shared.dtos;

import java.math.BigDecimal;

/**
 * Data Transfer Object representing a single item within an order.
 *
 * <p>Includes the movie details, quantity, pricing information, and the
 * derived subtotal.</p>
 */
public class OrderItemDTO {
    /** Order item identifier. */
    private Long id;

    /** Identifier of the movie being ordered. */
    private Long movieId;

    /** Title of the ordered movie. */
    private String movieTitle;

    /** Quantity ordered. */
    private Integer quantity;

    /** Unit price for the movie. */
    private BigDecimal unitPrice;

    /** Computed subtotal for the line item. */
    private BigDecimal subtotal;

    /**
     * Creates an empty order item DTO.
     */
    public OrderItemDTO() {}

    /**
     * Creates an order item DTO and computes the subtotal when possible.
     *
     * @param id order item identifier
     * @param movieId movie identifier
     * @param movieTitle movie title
     * @param quantity ordered quantity
     * @param unitPrice unit price
     */
    public OrderItemDTO(Long id, Long movieId, String movieTitle, Integer quantity, BigDecimal unitPrice) {
        this.id = id;
        this.movieId = movieId;
        this.movieTitle = movieTitle;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = unitPrice != null && quantity != null ? unitPrice.multiply(new java.math.BigDecimal(quantity)) : null;
    }

    /**
     * Returns the order item identifier.
     *
     * @return order item identifier
     */
    public Long getId() { return id; }

    /**
     * Sets the order item identifier.
     *
     * @param id order item identifier
     */
    public void setId(Long id) { this.id = id; }

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
     * Returns the movie title.
     *
     * @return movie title
     */
    public String getMovieTitle() { return movieTitle; }

    /**
     * Sets the movie title.
     *
     * @param movieTitle movie title
     */
    public void setMovieTitle(String movieTitle) { this.movieTitle = movieTitle; }

    /**
     * Returns the ordered quantity.
     *
     * @return quantity
     */
    public Integer getQuantity() { return quantity; }

    /**
     * Sets the ordered quantity.
     *
     * @param quantity ordered quantity
     */
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    /**
     * Returns the unit price.
     *
     * @return unit price
     */
    public BigDecimal getUnitPrice() { return unitPrice; }

    /**
     * Sets the unit price.
     *
     * @param unitPrice unit price
     */
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    /**
     * Returns the computed subtotal.
     *
     * @return subtotal
     */
    public BigDecimal getSubtotal() { return subtotal; }

    /**
     * Sets the subtotal explicitly.
     *
     * @param subtotal subtotal value
     */
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}
