package com.example.desofs.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.Map;

/**
 * Test configuration that replaces the real JwtDecoder with a mock
 * that does not contact Auth0.
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    @Primary
    public JwtDecoder jwtDecoder() {
        // The mock decoder is not used directly in tests since we use
        // SecurityMockMvcRequestPostProcessors.jwt() which bypasses decoding.
        // This bean exists to prevent Spring from trying to contact Auth0.
        return token -> Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .claim("sub", "test-subject")
                .claim("iss", "https://test.auth0.com/")
                .claim("aud", "emovieshop-api")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }
}

