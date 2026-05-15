package com.example.desofs.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    // OPAQUE-BOX TESTS : Focus on the external behavior of the class, without knowing how it is implemented.
    @Nested
    class OpaqueBoxTests {

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

        @Test
        @DisplayName("setStatus updates order status")
        void setStatus_updatesOrderStatus() {
            Order order = new Order("auth0|user1", "John Doe");

            order.setStatus(OrderStatus.REFUNDED);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);
        }

        @Test
        @DisplayName("setId updates order id")
        void setId_updatesOrderId() {
            Order order = new Order("auth0|user1", "John Doe");

            order.setId(123L);

            assertThat(order.getId()).isEqualTo(123L);
        }

        @Test
        @DisplayName("createdAt is close to now on creation")
        void createdAt_isCloseToNow() {
            LocalDateTime beforeCreation = LocalDateTime.now().minusSeconds(1);
            Order order = new Order("auth0|user1", "John Doe");
            LocalDateTime afterCreation = LocalDateTime.now().plusSeconds(1);

            assertThat(order.getCreatedAt()).isBetween(beforeCreation, afterCreation);
        }

        @Test
        @DisplayName("adding item through returned items list throws")
        void getItems_addOperation_throws() {
            Order order = new Order("auth0|user1", "John Doe");
            Movie movie = new Movie("Inception", "A mind-bending thriller", "Sci-Fi", "Blu-ray", new BigDecimal("14.99"), 10);

            List<OrderItem> items = order.getItems();

            assertThatThrownBy(() -> items.add(new OrderItem(movie, 1, new BigDecimal("14.99"))))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // TRANSPARENT-BOX TESTS : Inspect or manipulate internal state or behavior
    @Nested
    class TransparentBoxTests {

        @Test
        @DisplayName("Protected no-arg constructor exists for JPA")
        void protectedNoArgConstructor_exists() throws Exception {
            Constructor<Order> constructor = Order.class.getDeclaredConstructor();

            assertThat(java.lang.reflect.Modifier.isProtected(constructor.getModifiers())).isTrue();
        }

        @Test
        @DisplayName("Protected no-arg constructor creates empty entity")
        void protectedNoArgConstructor_createsEmptyEntity() throws Exception {
            Constructor<Order> constructor = Order.class.getDeclaredConstructor();
            constructor.setAccessible(true);

            Order order = constructor.newInstance();

            assertThat(order.getId()).isNull();
            assertThat(order.getAuth0Id()).isNull();
            assertThat(order.getItems()).isEmpty();
            assertThat(order.getStatus()).isNull();
            assertThat(order.getReceiptName()).isNull();
            assertThat(order.getTotalPrice()).isNull();
            assertThat(order.getCreatedAt()).isNull();
        }

        @Test
        @DisplayName("Private totalPrice can be manipulated through reflection")
        void privateTotalPrice_canBeManipulatedThroughReflection() throws Exception {
            Order order = new Order("auth0|user1", "John Doe");
            Field totalPriceField = Order.class.getDeclaredField("totalPrice");
            totalPriceField.setAccessible(true);

            totalPriceField.set(order, new BigDecimal("100.00"));

            assertThat(order.getTotalPrice()).isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("addItem uses item subtotal from internals")
        void addItem_usesOrderItemInternalSubtotal() throws Exception {
            Order order = new Order("auth0|user1", "John Doe");
            Movie movie = new Movie("Inception", "A mind-bending thriller", "Sci-Fi", "Blu-ray", new BigDecimal("14.99"), 10);
            OrderItem item = new OrderItem(movie, 1, new BigDecimal("14.99"));

            Field quantityField = OrderItem.class.getDeclaredField("quantity");
            Field unitPriceField = OrderItem.class.getDeclaredField("unitPrice");
            quantityField.setAccessible(true);
            unitPriceField.setAccessible(true);
            quantityField.set(item, 4);
            unitPriceField.set(item, new BigDecimal("2.50"));

            order.addItem(item);

            assertThat(order.getTotalPrice()).isEqualByComparingTo("10.00");
        }
    }
}
