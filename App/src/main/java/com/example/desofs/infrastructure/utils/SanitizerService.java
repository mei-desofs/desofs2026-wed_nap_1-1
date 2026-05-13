package com.example.desofs.infrastructure.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

/**
 * Sanitizer Service for cleaning and validating user input
 * To be implemented with XSS prevention, SQL injection prevention, etc.
 */
public class SanitizerService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    /**
     * Sanitize string input to prevent XSS attacks
     * @param input raw input string
     * @return sanitized string
     */
    public String sanitizeHtml(String input) {
        return sanitize(input)
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    /**
     * Sanitize SQL-like input to prevent SQL injection
     * @param input raw input string
     * @return sanitized string
     */
    public String sanitizeSql(String input) {
        if (input == null) {
            return null;
        }
        return input.replace("'", "''").replace(";", "").replace("--", "");
    }

    /**
     * Validate and sanitize email address
     * @param email email to validate
     * @return sanitized email or null if invalid
     */
    public String sanitizeEmail(String email) {
        if (email == null) {
            return null;
        }
        String normalized = email.trim().toLowerCase();
        return EMAIL_PATTERN.matcher(normalized).matches() ? normalized : null;
    }

    /**
     * Validate and sanitize URL
     * @param url URL to validate
     * @return sanitized URL or null if invalid
     */
    public String sanitizeUrl(String url) {
        if (url == null) {
            return null;
        }
        try {
            URI uri = new URI(url.trim());
            return uri.getScheme() != null && uri.getHost() != null ? uri.toString() : null;
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    /**
     * Generic sanitization for user text
     * @param input raw input string
     * @return sanitized string
     */
    public String sanitize(String input) {
        return input == null ? null : input.trim();
    }
}
