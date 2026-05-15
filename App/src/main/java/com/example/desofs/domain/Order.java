package com.example.desofs.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Entity representing a customer's order.
 *
 * <p>An {@code Order} contains one or more {@link OrderItem} entries, the
 * total price, status and metadata such as the creating user's identifier and
 * creation timestamp. Instances are persisted via JPA.</p>
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "auth0_id", nullable = false)
    private String auth0Id;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private List<OrderItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private String receiptName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Protected no-arg constructor required by JPA.
     *
     * <p>For framework use only.</p>
     */
    protected Order() {
    }

    /**
     * Create a new Order for the given user with the provided receipt name.
     *
     * @param auth0Id the identifier of the user who placed the order (required)
     * @param receiptName the name to appear on the order receipt (required)
     */
    public Order(String auth0Id, String receiptName) {
        this.auth0Id = auth0Id;
        this.receiptName = receiptName;
        this.status = OrderStatus.COMPLETED;
        this.totalPrice = BigDecimal.ZERO;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Add an item to this order and update the total price accordingly.
     *
     * @param item the {@link OrderItem} to add; must not be {@code null}
     */
    public void addItem(OrderItem item) {
        this.items.add(item);
        this.totalPrice = this.totalPrice.add(item.getSubtotal());
    }

    /**
     * Returns the database identifier for this order.
     *
     * @return the order id, or {@code null} if not yet persisted
     */
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns the identifier of the user who placed the order.
     *
     * @return the user's Auth0 id
     */
    public String getAuth0Id() {
        return auth0Id;
    }

    /**
     * Returns an unmodifiable view of the items in this order.
     *
     * @return the order items
     */

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    /**
     * Returns the current status of the order.
     *
     * @return the {@link OrderStatus}
     */
    public OrderStatus getStatus() {
        return status;
    }

    /**
     * Returns the name printed on the order receipt.
     *
     * @return the receipt name
     */
    public String getReceiptName() {
        return receiptName;
    }

    /**
     * Returns the total price for the order.
     *
     * @return the total price as a {@link BigDecimal}
     */
    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    /**
     * Returns the timestamp when the order was created.
     *
     * @return creation time (not {@code null})
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Update the order status.
     *
     * @param status the new {@link OrderStatus} to apply
     */
    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}
