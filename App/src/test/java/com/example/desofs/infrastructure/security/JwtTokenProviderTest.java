package com.example.desofs.infrastructure.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private final JwtTokenProvider provider = new JwtTokenProvider();

    @Test
    void shouldExtractClaimsFromUnsignedTokenLikeStructure() {
        String header = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("{}".getBytes());
        String payload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":\"123\",\"email\":\"user@example.com\",\"roles\":[\"ADMIN\"]}".getBytes());
        String token = header + "." + payload + ".signature";

        assertTrue(provider.validateToken(token));
        assertEquals("123", provider.getUserId(token).orElse(null));
        assertEquals("user@example.com", provider.getEmail(token).orElse(null));
        assertArrayEquals(new String[]{"ADMIN"}, provider.getUserRoles(token).orElseThrow());
    }
}
