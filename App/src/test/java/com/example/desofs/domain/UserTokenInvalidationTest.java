package com.example.desofs.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("UserTokenInvalidation Entity Tests")
class UserTokenInvalidationTest {

    @Test
    @DisplayName("no-arg constructor produces an empty entity (used by JPA)")
    void noArgConstructor() {
        UserTokenInvalidation entry = new UserTokenInvalidation();

        assertThat(entry.getAuth0UserId()).isNull();
        assertThat(entry.getInvalidatedAfter()).isNull();
        assertThat(entry.getReason()).isNull();
        assertThat(entry.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("all-args constructor populates id/cutoff/reason and stamps updatedAt")
    void allArgsConstructor() {
        Instant cutoff = Instant.parse("2026-01-01T00:00:00Z");

        UserTokenInvalidation entry = new UserTokenInvalidation(
                "auth0|user-1", cutoff, "ROLE_ASSIGNED:ADMIN");

        assertThat(entry.getAuth0UserId()).isEqualTo("auth0|user-1");
        assertThat(entry.getInvalidatedAfter()).isEqualTo(cutoff);
        assertThat(entry.getReason()).isEqualTo("ROLE_ASSIGNED:ADMIN");
        assertThat(entry.getUpdatedAt()).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("setters round-trip every field")
    void settersRoundTrip() {
        Instant cutoff = Instant.parse("2026-02-02T10:00:00Z");
        Instant updated = Instant.parse("2026-02-02T10:00:01Z");
        UserTokenInvalidation entry = new UserTokenInvalidation();

        entry.setAuth0UserId("auth0|x");
        entry.setInvalidatedAfter(cutoff);
        entry.setReason("ROLE_REMOVED:SUPPORT");
        entry.setUpdatedAt(updated);

        assertThat(entry.getAuth0UserId()).isEqualTo("auth0|x");
        assertThat(entry.getInvalidatedAfter()).isEqualTo(cutoff);
        assertThat(entry.getReason()).isEqualTo("ROLE_REMOVED:SUPPORT");
        assertThat(entry.getUpdatedAt()).isEqualTo(updated);
    }
}
