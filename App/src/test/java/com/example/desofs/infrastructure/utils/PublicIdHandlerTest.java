package com.example.desofs.infrastructure.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PublicIdHandlerTest {

    @Test
    void shouldEncodeAndDecodePositiveIds() {
        String encoded = PublicIdHandler.encode(123L);
        assertNotNull(encoded);
        assertTrue(PublicIdHandler.isValid(encoded));
        assertEquals(123L, PublicIdHandler.decode(encoded));
    }

    @Test
    void shouldReturnNullForInvalidDecodeInput() {
        assertNull(PublicIdHandler.decode("invalid-id-!"));
    }
}
