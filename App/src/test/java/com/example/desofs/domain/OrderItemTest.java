package com.example.desofs.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class OrderItemTest {

    // OPAQUE-BOX TESTS : Focus on the external behavior of the class, without knowing how it is implemented.
    @Nested
    class OpaqueBoxTests {

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

        @Test
        @DisplayName("Constructor keeps the same movie reference")
        void constructor_keepsSameMovieReference() {
            Movie movie = new Movie("Inception", "A mind-bending thriller", "Sci-Fi", "Blu-ray", new BigDecimal("14.99"), 10);
            OrderItem item = new OrderItem(movie, 2, new BigDecimal("14.99"));

            assertThat(item.getMovie()).isSameAs(movie);
        }

        @Test
        @DisplayName("getSubtotal works with decimal multiplication")
        void getSubtotal_decimalMultiplication() {
            Movie movie = new Movie("Dune", "Epic sci-fi", "Sci-Fi", "4K", new BigDecimal("19.99"), 8);
            OrderItem item = new OrderItem(movie, 2, new BigDecimal("19.99"));

            assertThat(item.getSubtotal()).isEqualByComparingTo("39.98");
        }

        @Test
        @DisplayName("getSubtotal returns zero when quantity is zero")
        void getSubtotal_zeroQuantity_returnsZero() {
            Movie movie = new Movie("Dune", "Epic sci-fi", "Sci-Fi", "4K", new BigDecimal("19.99"), 8);
            OrderItem item = new OrderItem(movie, 0, new BigDecimal("19.99"));

            assertThat(item.getSubtotal()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // TRANSPARENT-BOX TESTS : Inspect or manipulate internal state or behavior
    @Nested
    class TransparentBoxTests {

        @Test
        @DisplayName("Protected no-arg constructor exists for JPA")
        void protectedNoArgConstructor_exists() throws Exception {
            Constructor<OrderItem> constructor = OrderItem.class.getDeclaredConstructor();

            assertThat(java.lang.reflect.Modifier.isProtected(constructor.getModifiers())).isTrue();
        }

        @Test
        @DisplayName("Protected no-arg constructor creates an empty entity instance")
        void protectedNoArgConstructor_createsEmptyEntity() throws Exception {
            Constructor<OrderItem> constructor = OrderItem.class.getDeclaredConstructor();
            constructor.setAccessible(true);

            OrderItem item = constructor.newInstance();

            assertThat(item.getMovie()).isNull();
            assertThat(item.getQuantity()).isNull();
            assertThat(item.getUnitPrice()).isNull();
        }

        @Test
        @DisplayName("Private fields can be modified through reflection")
        void privateFields_canBeModifiedThroughReflection() throws Exception {
            OrderItem item = new OrderItem(null, 1, new BigDecimal("9.99"));
            Field quantityField = OrderItem.class.getDeclaredField("quantity");
            Field unitPriceField = OrderItem.class.getDeclaredField("unitPrice");
            quantityField.setAccessible(true);
            unitPriceField.setAccessible(true);

            quantityField.set(item, 4);
            unitPriceField.set(item, new BigDecimal("2.50"));

            assertThat(item.getQuantity()).isEqualTo(4);
            assertThat(item.getUnitPrice()).isEqualByComparingTo("2.50");
            assertThat(item.getSubtotal()).isEqualByComparingTo("10.00");
        }

        @Test
        @DisplayName("Different internal values produce the expected subtotal")
        void internalValues_produceExpectedSubtotal() {
            Movie movie = new Movie("Interstellar", "Space exploration", "Sci-Fi", "Blu-ray", new BigDecimal("15.00"), 12);
            OrderItem item = new OrderItem(movie, 3, new BigDecimal("15.00"));

            assertThat(item.getSubtotal()).isEqualByComparingTo("45.00");
        }
    }
}
