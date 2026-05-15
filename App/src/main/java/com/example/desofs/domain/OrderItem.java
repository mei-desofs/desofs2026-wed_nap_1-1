package com.example.desofs.domain;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Entity representing a single item within an {@link Order}.
 *
 * <p>An {@code OrderItem} refers to a purchased {@link Movie}, the quantity
 * ordered and the unit price. It provides a subtotal calculation used when
 * computing the parent order's total price. Instances are persisted by JPA.</p>
 */
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    /**
     * Protected no-arg constructor required by JPA.
     *
     * <p>For framework use only.</p>
     */
    protected OrderItem() {
        
    }

    /**
     * Create a new order item.
     *
     * @param movie the movie being ordered (required)
     * @param quantity the quantity ordered (required, positive)
     * @param unitPrice the price per unit (required)
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Movie is a JPA entity; defensive copy would break ORM identity and lazy loading")
    public OrderItem(Movie movie, Integer quantity, BigDecimal unitPrice) {
        this.movie = movie;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    /**
     * Returns the movie associated with this order item.
     *
     * @return the {@link Movie}
     */

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Movie is a JPA entity; defensive copy would break ORM identity and lazy loading")
    public Movie getMovie() {
        return movie;
    }

    /**
     * Returns the quantity ordered for the movie.
     *
     * @return the quantity (non-negative)
     */
    public Integer getQuantity() {
        return quantity;
    }

    /**
     * Returns the unit price for this order item.
     * @return the unit price as a {@link BigDecimal}
     */
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    /**
     * Calculate the subtotal for this order item (unit price multiplied by quantity).
     *
     * @return the subtotal as a {@link BigDecimal}
     */
    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
