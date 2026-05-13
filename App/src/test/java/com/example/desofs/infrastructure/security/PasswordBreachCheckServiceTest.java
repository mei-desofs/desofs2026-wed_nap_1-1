package com.example.desofs.infrastructure.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordBreachCheckServiceTest {

    private final PasswordBreachCheckService service = new PasswordBreachCheckService();

    @Test
    void shouldDetectCommonBreachedPasswords() {
        assertTrue(service.isPasswordBreached("password"));
        assertEquals(1, service.getBreachCount("password"));
    }

    @Test
    void shouldNotDetectUnknownPassword() {
        assertFalse(service.isPasswordBreached("ThisIsAUniquePassword123!"));
        assertEquals(0, service.getBreachCount("ThisIsAUniquePassword123!"));
    }
}
