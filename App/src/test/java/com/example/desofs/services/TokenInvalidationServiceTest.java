package com.example.desofs.services;

import com.example.desofs.domain.UserTokenInvalidation;
import com.example.desofs.repositories.UserTokenInvalidationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenInvalidationService Unit Tests")
class TokenInvalidationServiceTest {

    private static final String USER = "auth0|user-1";

    @Mock
    private UserTokenInvalidationRepository repository;

    @InjectMocks
    private TokenInvalidationService service;

    @BeforeEach
    void resetMocks() {
        reset(repository);
    }

    @Test
    @DisplayName("invalidateTokensFor inserts a new denylist entry when none exists")
    void invalidateTokensFor_insertsWhenMissing() {
        when(repository.findById(USER)).thenReturn(Optional.empty());

        service.invalidateTokensFor(USER, "ROLE_ASSIGNED:ADMIN");

        ArgumentCaptor<UserTokenInvalidation> captor =
                ArgumentCaptor.forClass(UserTokenInvalidation.class);
        verify(repository).save(captor.capture());
        UserTokenInvalidation saved = captor.getValue();
        assertThat(saved.getAuth0UserId()).isEqualTo(USER);
        assertThat(saved.getReason()).isEqualTo("ROLE_ASSIGNED:ADMIN");
        assertThat(saved.getInvalidatedAfter()).isCloseTo(Instant.now(),
                within(5, ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("invalidateTokensFor refreshes the cutoff on an existing entry")
    void invalidateTokensFor_refreshesExisting() {
        Instant old = Instant.now().minusSeconds(3600);
        UserTokenInvalidation existing =
                new UserTokenInvalidation(USER, old, "OLD_REASON");
        when(repository.findById(USER)).thenReturn(Optional.of(existing));

        service.invalidateTokensFor(USER, "NEW_REASON");

        verify(repository).save(existing);
        assertThat(existing.getReason()).isEqualTo("NEW_REASON");
        assertThat(existing.getInvalidatedAfter()).isAfter(old);
    }

    @Test
    @DisplayName("isTokenInvalidated returns true when iat predates cutoff")
    void isTokenInvalidated_iatBeforeCutoff() {
        Instant cutoff = Instant.now();
        when(repository.findById(USER)).thenReturn(Optional.of(
                new UserTokenInvalidation(USER, cutoff, "ANY")));

        boolean rejected = service.isTokenInvalidated(USER, cutoff.minusSeconds(1));

        assertThat(rejected).isTrue();
    }

    @Test
    @DisplayName("isTokenInvalidated returns false when iat equals or exceeds cutoff")
    void isTokenInvalidated_iatAfterCutoff() {
        Instant cutoff = Instant.now();
        when(repository.findById(USER)).thenReturn(Optional.of(
                new UserTokenInvalidation(USER, cutoff, "ANY")));

        assertThat(service.isTokenInvalidated(USER, cutoff)).isFalse();
        assertThat(service.isTokenInvalidated(USER, cutoff.plusSeconds(1))).isFalse();
    }

    @Test
    @DisplayName("isTokenInvalidated returns false when user has no denylist entry")
    void isTokenInvalidated_noEntry() {
        when(repository.findById(USER)).thenReturn(Optional.empty());

        assertThat(service.isTokenInvalidated(USER, Instant.now())).isFalse();
    }

    @Test
    @DisplayName("isTokenInvalidated tolerates null inputs without DB access")
    void isTokenInvalidated_nullInputs() {
        assertThat(service.isTokenInvalidated(null, Instant.now())).isFalse();
        assertThat(service.isTokenInvalidated(USER, null)).isFalse();
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("getInvalidatedAfter returns recorded cutoff")
    void getInvalidatedAfter_returnsCutoff() {
        Instant cutoff = Instant.now();
        when(repository.findById(USER)).thenReturn(Optional.of(
                new UserTokenInvalidation(USER, cutoff, "ANY")));

        assertThat(service.getInvalidatedAfter(USER)).contains(cutoff);
    }

    @Test
    @DisplayName("getInvalidatedAfter returns empty for unknown user / blank id")
    void getInvalidatedAfter_unknownUser() {
        when(repository.findById(USER)).thenReturn(Optional.empty());

        assertThat(service.getInvalidatedAfter(USER)).isEmpty();
        assertThat(service.getInvalidatedAfter(null)).isEmpty();
        assertThat(service.getInvalidatedAfter("")).isEmpty();
    }

    @Test
    @DisplayName("invalidateTokensFor rejects null or blank user id")
    void invalidateTokensFor_blankUserId() {
        assertThatThrownBy(() -> service.invalidateTokensFor("", "X"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.invalidateTokensFor(null, "X"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("invalidateTokensFor normalises blank reason to UNSPECIFIED")
    void invalidateTokensFor_blankReason() {
        when(repository.findById(USER)).thenReturn(Optional.empty());

        service.invalidateTokensFor(USER, "");

        ArgumentCaptor<UserTokenInvalidation> captor =
                ArgumentCaptor.forClass(UserTokenInvalidation.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getReason()).isEqualTo("UNSPECIFIED");
    }
}
