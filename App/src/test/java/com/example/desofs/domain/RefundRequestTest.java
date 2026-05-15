package com.example.desofs.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RefundRequestTest {

    // OPAQUE-BOX TESTS : Focus on the external behavior of the class, without knowing how it is implemented.
    @Nested
    class OpaqueBoxTests {

        @Test
        @DisplayName("Constructor initializes status and timestamps")
        void constructor_initializesDefaults() {
            LocalDateTime beforeCreation = LocalDateTime.now().minusSeconds(1);
            RefundRequest refundRequest = new RefundRequest();
            LocalDateTime afterCreation = LocalDateTime.now().plusSeconds(1);

            assertThat(refundRequest.getId()).isNull();
            assertThat(refundRequest.getStatus()).isEqualTo(RefundRequest.RefundStatus.REQUESTED);
            assertThat(refundRequest.getCreatedAt()).isBetween(beforeCreation, afterCreation);
            assertThat(refundRequest.getUpdatedAt()).isBetween(beforeCreation, afterCreation);
        }

        @Test
        @DisplayName("Factory method sets all provided fields")
        void of_setsAllFields() {
            Order order = new Order("auth0|user1", "John Doe");
            RefundRequest refundRequest = new RefundRequest(order, "auth0|customer", new BigDecimal("29.98"), "Wrong item");

            assertThat(refundRequest.getOrder()).isEqualTo(order);
            assertThat(refundRequest.getAuth0Id()).isEqualTo("auth0|customer");
            assertThat(refundRequest.getAmount()).isEqualByComparingTo("29.98");
            assertThat(refundRequest.getReason()).isEqualTo("Wrong item");
            assertThat(refundRequest.getStatus()).isEqualTo(RefundRequest.RefundStatus.REQUESTED);
            assertThat(refundRequest.getCreatedAt()).isNotNull();
            assertThat(refundRequest.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Setters update mutable fields")
        void setters_updateFields() {
            RefundRequest refundRequest = new RefundRequest();
            Order order = new Order("auth0|user1", "John Doe");

            refundRequest.setOrder(order);
            refundRequest.setAuth0Id("auth0|actor");
            refundRequest.setAmount(new BigDecimal("10.00"));
            refundRequest.setReason("Changed reason");
            refundRequest.setStatus(RefundRequest.RefundStatus.APPROVED);

            assertThat(refundRequest.getOrder()).isEqualTo(order);
            assertThat(refundRequest.getAuth0Id()).isEqualTo("auth0|actor");
            assertThat(refundRequest.getAmount()).isEqualByComparingTo("10.00");
            assertThat(refundRequest.getReason()).isEqualTo("Changed reason");
            assertThat(refundRequest.getStatus()).isEqualTo(RefundRequest.RefundStatus.APPROVED);
        }

        @Test
        @DisplayName("Factory supports rejected status later via setter")
        void status_canBeRejected() {
            RefundRequest refundRequest = new RefundRequest();

            refundRequest.setStatus(RefundRequest.RefundStatus.REJECTED);

            assertThat(refundRequest.getStatus()).isEqualTo(RefundRequest.RefundStatus.REJECTED);
        }

    }

    // TRANSPARENT-BOX TESTS : Inspect or manipulate internal state or behavior
    @Nested
    class TransparentBoxTests {

        @Test
        @DisplayName("Protected no-arg constructor creates a blank JPA entity")
        void protectedNoArgConstructor_createsEmptyEntity() throws Exception {
            Constructor<RefundRequest> constructor = RefundRequest.class.getDeclaredConstructor();
            RefundRequest refundRequest = constructor.newInstance();

            assertThat(refundRequest.getId()).isNull();
            assertThat(refundRequest.getOrder()).isNull();
            assertThat(refundRequest.getAuth0Id()).isNull();
            assertThat(refundRequest.getAmount()).isNull();
            assertThat(refundRequest.getReason()).isNull();
            assertThat(refundRequest.getStatus()).isEqualTo(RefundRequest.RefundStatus.REQUESTED);
            assertThat(refundRequest.getCreatedAt()).isNotNull();
            assertThat(refundRequest.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Private id field can be set via reflection")
        void privateId_canBeSetViaReflection() throws Exception {
            RefundRequest refundRequest = new RefundRequest();
            Field idField = RefundRequest.class.getDeclaredField("id");
            idField.setAccessible(true);

            idField.set(refundRequest, 55L);

            assertThat(refundRequest.getId()).isEqualTo(55L);
        }

        @Test
        @DisplayName("setStatus refreshes updatedAt timestamp")
        void setStatus_refreshesUpdatedAt() throws Exception {
            RefundRequest refundRequest = new RefundRequest();
            Field updatedAtField = RefundRequest.class.getDeclaredField("updatedAt");
            updatedAtField.setAccessible(true);
            LocalDateTime previousUpdatedAt = (LocalDateTime) updatedAtField.get(refundRequest);

            refundRequest.setStatus(RefundRequest.RefundStatus.APPROVED);

            assertThat(refundRequest.getUpdatedAt()).isAfterOrEqualTo(previousUpdatedAt);
            assertThat(refundRequest.getStatus()).isEqualTo(RefundRequest.RefundStatus.APPROVED);
        }

    }

}