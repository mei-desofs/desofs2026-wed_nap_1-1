package com.example.desofs.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AudienceValidatorTest {

    private AudienceValidator validator;
    private static final String EXPECTED_AUDIENCE = "https://emovieshop-api";

    @BeforeEach
    void setUp() {
        validator = new AudienceValidator(EXPECTED_AUDIENCE);
    }

    @Test
    @DisplayName("JWT with correct audience passes validation")
    void validate_correctAudience_success() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "auth0|user1")
                .audience(List.of(EXPECTED_AUDIENCE))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertFalse(result.hasErrors());
    }

    @Test
    @DisplayName("JWT with multiple audiences including expected one passes")
    void validate_multipleAudiencesIncludingExpected_success() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "auth0|user1")
                .audience(List.of("https://other-api", EXPECTED_AUDIENCE))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertFalse(result.hasErrors());
    }

    @Test
    @DisplayName("JWT with wrong audience fails validation")
    void validate_wrongAudience_fails() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "auth0|user1")
                .audience(List.of("https://wrong-api"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertTrue(result.hasErrors());
    }

    @Test
    @DisplayName("JWT with no audience fails validation")
    void validate_noAudience_fails() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "auth0|user1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertTrue(result.hasErrors());
    }

    @Test
    @DisplayName("JWT with empty audience list fails validation")
    void validate_emptyAudience_fails() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "auth0|user1")
                .audience(List.of())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertTrue(result.hasErrors());
    }

    @Test
    @DisplayName("Audience validation is case-sensitive")
    void validate_caseSensitive_fails() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "auth0|user1")
                .audience(List.of("HTTPS://EMOVIESHOP-API"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertTrue(result.hasErrors());
    }
}

