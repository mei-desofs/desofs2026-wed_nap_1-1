package com.example.desofs.infrastructure.security;

import org.springframework.stereotype.Component;
import java.util.Set;

/**
 * Password Breach Check Service to verify passwords against known breaches
 * To be implemented with integration to Have I Been Pwned API or similar service
 */
@Component
public class PasswordBreachCheckService {

    private static final Set<String> COMMON_BREACHED_PASSWORDS = Set.of(
        "password", "123456", "123456789", "qwerty", "letmein", "admin", "welcome"
    );

    /**
     * Check if password has been found in known breaches
     * @param password password to check
     * @return true if password is breached, false if safe
     */
    public boolean isPasswordBreached(String password) {
        return password != null && COMMON_BREACHED_PASSWORDS.contains(password.toLowerCase());
    }

    /**
     * Get breach count for a password
     * @param password password to check
     * @return number of times password appears in breach databases
     */
    public int getBreachCount(String password) {
        return isPasswordBreached(password) ? 1 : 0;
    }
}
