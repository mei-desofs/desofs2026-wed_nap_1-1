package com.example.desofs.domain.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {

    @Test
    @DisplayName("Constructor initializes order with correct defaults")
    void constructor_initializesDefaults() {
        Order order = new Order("auth0|user1", "John Doe");

        assertThat(order.getAuth0Id()).isEqualTo("auth0|user1");
        assertThat(order.getReceiptName()).isEqualTo("John Doe");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(order.getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(order.getItems()).isEmpty();
        assertThat(order.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("addItem adds item and updates total price")
    void addItem_updatesTotal() {
        Order order = new Order("auth0|user1", "John Doe");
        Movie movie = new Movie("Inception", "A mind-bending thriller", "Sci-Fi", "Blu-ray", new BigDecimal("14.99"), 10);
        OrderItem item = new OrderItem(movie, 2, new BigDecimal("14.99"));

        order.addItem(item);

        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getTotalPrice()).isEqualByComparingTo("29.98");
    }

    @Test
    @DisplayName("addItem accumulates total across multiple items")
    void addItem_multipleItems_accumulatesTotal() {
        Order order = new Order("auth0|user1", "John Doe");
        Movie movie1 = new Movie("Inception", "A mind-bending thriller", "Sci-Fi", "Blu-ray", new BigDecimal("14.99"), 10);
        Movie movie2 = new Movie("The Matrix", "A computer hacker learns about the true nature of reality", "Sci-Fi", "DVD", new BigDecimal("12.99"), 5);

        order.addItem(new OrderItem(movie1, 1, new BigDecimal("14.99")));
        order.addItem(new OrderItem(movie2, 1, new BigDecimal("12.99")));

        assertThat(order.getItems()).hasSize(2);
        assertThat(order.getTotalPrice()).isEqualByComparingTo("27.98");
    }

    @Test
    @DisplayName("getItems returns unmodifiable list")
    void getItems_returnsUnmodifiableList() {
        Order order = new Order("auth0|user1", "John Doe");
        Movie movie = new Movie("Inception", "A mind-bending thriller", "Sci-Fi", "Blu-ray", new BigDecimal("14.99"), 10);
        order.addItem(new OrderItem(movie, 1, new BigDecimal("14.99")));

        assertThat(order.getItems()).isUnmodifiable();
    }
}
