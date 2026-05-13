package com.example.desofs.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
/**
 * Entity representing a single item within an {@link Order}.
 *
 * <p>An {@code OrderItem} refers to a purchased {@link Movie}, the quantity
 * ordered and the unit price. It provides a subtotal calculation used when
 * computing the parent order's total price. Instances are persisted by JPA.</p>
 */
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

    protected OrderItem() {
    }

    /**
     * Protected no-arg constructor required by JPA.
     *
     * <p>For framework use only.</p>
     */

    public OrderItem(Movie movie, Integer quantity, BigDecimal unitPrice) {
        this.movie = movie;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    /**
     * Create a new order item.
     *
     * @param movie the movie being ordered (required)
     * @param quantity the quantity ordered (required, positive)
     * @param unitPrice the price per unit (required)
     */

    public Long getId() {
        return id;
    }

    /**
     * Returns the database identifier for this order item.
     *
     * @return the id, or {@code null} if not yet persisted
     */

    public Movie getMovie() {
        return movie;
    }

    /**
     * Returns the movie associated with this order item.
     *
     * @return the {@link Movie}
     */

    public Integer getQuantity() {
        return quantity;
    }

    /**
     * Returns the quantity ordered for the movie.
     *
     * @return the quantity (non-negative)
     */

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    /**
     * Returns the unit price for this order item.
     *
     * @return the unit price as a {@link BigDecimal}
     */

    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * Calculate the subtotal for this order item (unit price multiplied by quantity).
     *
     * @return the subtotal as a {@link BigDecimal}
     */
}
