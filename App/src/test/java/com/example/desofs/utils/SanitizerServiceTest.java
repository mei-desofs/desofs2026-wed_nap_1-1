package com.example.desofs.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SanitizerServiceTest {

    private final SanitizerService sanitizerService = new SanitizerService();

    @Test
    void shouldSanitizeHtml() {
        assertEquals("&lt;script&gt;alert(1)&lt;/script&gt;", sanitizerService.sanitizeHtml("<script>alert(1)</script>"));
    }

    @Test
    void shouldValidateEmail() {
        assertEquals("test@example.com", sanitizerService.sanitizeEmail("  Test@Example.com  "));
        assertNull(sanitizerService.sanitizeEmail("not-an-email"));
    }
}
