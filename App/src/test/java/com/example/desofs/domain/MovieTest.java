package com.example.desofs.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MovieTest {

    // OPAQUE-BOX TESTS : Focus on the external behavior of the class, without knowing how it is implemented.
    @Nested
    class OpaqueBoxTests {
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
        @DisplayName("Constructor allows nullable description and platform")
        void constructor_allowsNullableDescriptionAndPlatform() {
            Movie movie = new Movie("Inception", null, "Sci-Fi", null, new BigDecimal("14.99"), 10);

            assertThat(movie.getDescription()).isNull();
            assertThat(movie.getPlatform()).isNull();
        }

        @Test
        @DisplayName("getId is null before persistence")
        void getId_initiallyNull() {
            Movie movie = new Movie("Inception", "A mind-bending thriller", "Sci-Fi", "Blu-ray", new BigDecimal("14.99"), 10);

            assertThat(movie.getId()).isNull();
        }

        @Test
        @DisplayName("setId updates identifier")
        void setId_updatesIdentifier() {
            Movie movie = new Movie("Inception", "A mind-bending thriller", "Sci-Fi", "Blu-ray", new BigDecimal("14.99"), 10);

            movie.setId(42L);

            assertThat(movie.getId()).isEqualTo(42L);
        }
    }

    // TRANSPARENT-BOX TESTS : Inspect or manipulate internal state or behavior
    @Nested
    class TransparentBoxTests {

        @Test
        @DisplayName("Protected no-arg constructor exists for JPA")
        void protectedNoArgConstructor_exists() throws Exception {
            Constructor<Movie> constructor = Movie.class.getDeclaredConstructor();

            assertThat(constructor.getModifiers() & java.lang.reflect.Modifier.PROTECTED)
                    .isNotZero();
        }

        @Test
        @DisplayName("Protected no-arg constructor creates an empty entity instance")
        void protectedNoArgConstructor_createsEmptyEntity() throws Exception {
            Constructor<Movie> constructor = Movie.class.getDeclaredConstructor();
            constructor.setAccessible(true);

            Movie movie = constructor.newInstance();

            assertThat(movie.getId()).isNull();
            assertThat(movie.getTitle()).isNull();
            assertThat(movie.getDescription()).isNull();
            assertThat(movie.getGenre()).isNull();
            assertThat(movie.getPlatform()).isNull();
            assertThat(movie.getPrice()).isNull();
            assertThat(movie.getStockQuantity()).isNull();
        }

        @Test
        @DisplayName("decreaseStock handles exact stock as an internal boundary case")
        void decreaseStock_exactStockBoundary() {
            Movie movie = new Movie("Inception", "A mind-bending thriller", "Sci-Fi", "Blu-ray", new BigDecimal("14.99"), 4);

            movie.decreaseStock(4);

            assertThat(movie.getStockQuantity()).isZero();
        }

        @Test
        @DisplayName("Repeated stock reductions update the internal quantity consistently")
        void decreaseStock_repeatedCallsUpdateState() {
            Movie movie = new Movie("Inception", "A mind-bending thriller", "Sci-Fi", "Blu-ray", new BigDecimal("14.99"), 10);

            movie.decreaseStock(2);
            movie.decreaseStock(3);

            assertThat(movie.getStockQuantity()).isEqualTo(5);
        }
    }
}
