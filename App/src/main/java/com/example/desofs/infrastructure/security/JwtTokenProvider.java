package com.example.desofs.infrastructure.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

/**
 * JWT Token Provider for Auth0 token validation and claim extraction
 * To be implemented with JWT parsing logic and claim validation
 */
@Component
public class JwtTokenProvider {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Validate JWT token from Auth0
     * @param token JWT token string
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        return extractPayload(token).isPresent();
    }

    /**
     * Extract claims from JWT token
     * @param token JWT token string
     * @return Map of claims
     */
    public Map<String, Object> extractClaims(String token) {
        return extractPayload(token).orElse(Map.of());
    }

    /**
     * Get user ID (sub claim) from token
     * @param token JWT token string
     * @return user ID
     */
    public Optional<String> getUserId(String token) {
        return extractPayload(token).map(claims -> valueAsString(claims.get("sub")));
    }

    /**
     * Get user roles from token
     * @param token JWT token string
     * @return array of role strings
     */
    public Optional<String[]> getUserRoles(String token) {
        return extractPayload(token).map(claims -> {
            Object roles = claims.get("roles");
            if (roles instanceof java.util.Collection<?> collection) {
                return collection.stream().map(String::valueOf).toArray(String[]::new);
            }
            Object permissions = claims.get("permissions");
            if (permissions instanceof java.util.Collection<?> collection) {
                return collection.stream().map(String::valueOf).toArray(String[]::new);
            }
            return new String[0];
        });
    }

    /**
     * Get email from token
     * @param token JWT token string
     * @return email address
     */
    public Optional<String> getEmail(String token) {
        return extractPayload(token).map(claims -> valueAsString(claims.get("email")));
    }

    private Optional<Map<String, Object>> extractPayload(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            return Optional.empty();
        }

        try {
            byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
            Map<String, Object> claims = objectMapper.readValue(decoded, new TypeReference<>() {});
            return Optional.of(claims);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private String valueAsString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
