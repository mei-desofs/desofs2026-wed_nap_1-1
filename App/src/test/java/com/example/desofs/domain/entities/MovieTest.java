package com.example.desofs.domain.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.desofs.domain.Movie;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MovieTest {

    @Test
    @DisplayName("Constructor sets all fields correctly")
    void constructor_setsFields() {
        Movie movie = new Movie("Inception", "A mind-bending thriller", "Sci-Fi", "Blu-ray", new BigDecimal("14.99"), 10);

        assertThat(movie.getTitle()).isEqualTo("Inception");
        assertThat(movie.getDescription()).isEqualTo("A mind-bending thriller");
        assertThat(movie.getGenre()).isEqualTo("Sci-Fi");
        assertThat(movie.getPlatform()).isEqualTo("Blu-ray");
        assertThat(movie.getPrice()).isEqualByComparingTo("14.99");
        assertThat(movie.getStockQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("decreaseStock reduces stock by given quantity")
    void decreaseStock_reducesStock() {
        Movie movie = new Movie("Inception", "A mind-bending thriller", "Sci-Fi", "Blu-ray", new BigDecimal("14.99"), 10);

        movie.decreaseStock(3);

        assertThat(movie.getStockQuantity()).isEqualTo(7);
    }

    @Test
    @DisplayName("decreaseStock to zero is allowed")
    void decreaseStock_toZero_allowed() {
        Movie movie = new Movie("Inception", "A mind-bending thriller", "Sci-Fi", "Blu-ray", new BigDecimal("14.99"), 5);

        movie.decreaseStock(5);

        assertThat(movie.getStockQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("decreaseStock throws when quantity is zero")
    void decreaseStock_zeroQuantity_throws() {
        Movie movie = new Movie("Inception", "A mind-bending thriller", "Sci-Fi", "Blu-ray", new BigDecimal("14.99"), 10);

        assertThatThrownBy(() -> movie.decreaseStock(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    @DisplayName("decreaseStock throws when quantity is negative")
    void decreaseStock_negativeQuantity_throws() {
        Movie movie = new Movie("Inception", "A mind-bending thriller", "Sci-Fi", "Blu-ray", new BigDecimal("14.99"), 10);

        assertThatThrownBy(() -> movie.decreaseStock(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    @DisplayName("decreaseStock throws when insufficient stock")
    void decreaseStock_insufficientStock_throws() {
        Movie movie = new Movie("Inception", "A mind-bending thriller", "Sci-Fi", "Blu-ray", new BigDecimal("14.99"), 2);

        assertThatThrownBy(() -> movie.decreaseStock(5))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient stock");
    }
}
