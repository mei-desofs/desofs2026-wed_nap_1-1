package com.example.desofs.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class OrderItemTest {

    @Test
    @DisplayName("Constructor sets all fields correctly")
    void constructor_setsFields() {
        Movie movie = new Movie("Inception", "A mind-bending thriller", "Sci-Fi", "Blu-ray", new BigDecimal("14.99"), 10);
        OrderItem item = new OrderItem(movie, 3, new BigDecimal("14.99"));

        assertThat(item.getMovie()).isEqualTo(movie);
        assertThat(item.getQuantity()).isEqualTo(3);
        assertThat(item.getUnitPrice()).isEqualByComparingTo("14.99");
    }

    @Test
    @DisplayName("getSubtotal returns quantity * unitPrice")
    void getSubtotal_calculatesCorrectly() {
        Movie movie = new Movie("Inception", "A mind-bending thriller", "Sci-Fi", "Blu-ray", new BigDecimal("14.99"), 10);
        OrderItem item = new OrderItem(movie, 3, new BigDecimal("14.99"));

        assertThat(item.getSubtotal()).isEqualByComparingTo("44.97");
    }

    @Test
    @DisplayName("getSubtotal with quantity 1 equals unitPrice")
    void getSubtotal_singleQuantity_equalsUnitPrice() {
        Movie movie = new Movie("The Matrix", "A computer hacker learns about the true nature of reality", "Sci-Fi", "DVD", new BigDecimal("12.99"), 5);
        OrderItem item = new OrderItem(movie, 1, new BigDecimal("12.99"));

        assertThat(item.getSubtotal()).isEqualByComparingTo("12.99");
    }
}
