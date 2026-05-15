package com.example.desofs.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import java.time.temporal.ChronoUnit;

class AuditLogTest {

    @Test
    @DisplayName("Default constructor sets timestamp close to now")
    void constructor_setsTimestampToNow() {
        LocalDateTime before = LocalDateTime.now();
        AuditLog log = new AuditLog();
        LocalDateTime after = LocalDateTime.now();

        assertThat(log.getTimestamp()).isNotNull();
        assertThat(log.getTimestamp()).isAfterOrEqualTo(before);
        assertThat(log.getTimestamp()).isBeforeOrEqualTo(after);
    }

    @Test
    @DisplayName("of() sets actorId, targetUserId, role and operation")
    void of_setsAllFields() {
        AuditLog log = AuditLog.of("auth0|actor", "auth0|target", Role.ADMIN, "ASSIGN");

        assertThat(log.getActorId()).isEqualTo("auth0|actor");
        assertThat(log.getTargetUserId()).isEqualTo("auth0|target");
        assertThat(log.getRole()).isEqualTo(Role.ADMIN);
        assertThat(log.getOperation()).isEqualTo("ASSIGN");
    }

    @Test
    @DisplayName("of() sets timestamp close to now")
    void of_setsTimestamp() {
        LocalDateTime before = LocalDateTime.now();
        AuditLog log = AuditLog.of("auth0|actor", "auth0|target", Role.SUPPORT, "REMOVE");
        LocalDateTime after = LocalDateTime.now();

        assertThat(log.getTimestamp()).isNotNull();
        assertThat(log.getTimestamp()).isCloseTo(before, within(1, ChronoUnit.SECONDS));
        assertThat(log.getTimestamp()).isBeforeOrEqualTo(after);
    }

    @Test
    @DisplayName("of() works for ASSIGN operation with CUSTOMER role")
    void of_assignCustomer() {
        AuditLog log = AuditLog.of("auth0|admin", "auth0|user", Role.CUSTOMER, "ASSIGN");

        assertThat(log.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(log.getOperation()).isEqualTo("ASSIGN");
    }

    @Test
    @DisplayName("of() works for REMOVE operation with SUPPORT role")
    void of_removeSupport() {
        AuditLog log = AuditLog.of("auth0|admin", "auth0|staff", Role.SUPPORT, "REMOVE");

        assertThat(log.getRole()).isEqualTo(Role.SUPPORT);
        assertThat(log.getOperation()).isEqualTo("REMOVE");
    }

    @Test
    @DisplayName("setActorId updates actorId")
    void setActorId_updatesField() {
        AuditLog log = new AuditLog();
        log.setActorId("auth0|newactor");

        assertThat(log.getActorId()).isEqualTo("auth0|newactor");
    }

    @Test
    @DisplayName("setTargetUserId updates targetUserId")
    void setTargetUserId_updatesField() {
        AuditLog log = new AuditLog();
        log.setTargetUserId("auth0|newtarget");

        assertThat(log.getTargetUserId()).isEqualTo("auth0|newtarget");
    }

    @Test
    @DisplayName("setRole updates role")
    void setRole_updatesField() {
        AuditLog log = new AuditLog();
        log.setRole(Role.ADMIN);

        assertThat(log.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("setOperation updates operation")
    void setOperation_updatesField() {
        AuditLog log = new AuditLog();
        log.setOperation("ASSIGN");

        assertThat(log.getOperation()).isEqualTo("ASSIGN");
    }

    @Test
    @DisplayName("getId returns null before persistence")
    void getId_nullBeforePersistence() {
        AuditLog log = AuditLog.of("auth0|a", "auth0|b", Role.ADMIN, "ASSIGN");

        assertThat(log.getId()).isNull();
    }
}
