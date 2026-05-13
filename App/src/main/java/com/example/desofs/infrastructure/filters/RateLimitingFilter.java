package com.example.desofs.infrastructure.filters;

import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.stereotype.Component;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting Filter to prevent abuse and DoS attacks
 * To be implemented with Bucket4j, Redis, or in-memory rate limiter
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int REQUEST_LIMIT = 100;
    private static final long WINDOW_SECONDS = 60;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        String clientIdentifier = getClientIdentifier(request);
        if (!checkRateLimit(clientIdentifier)) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"rate_limit_exceeded\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Check rate limit for client
     * @param clientIdentifier IP address or user ID
     * @return true if under limit, false if exceeded
     */
    private boolean checkRateLimit(String clientIdentifier) {
        Window window = windows.computeIfAbsent(clientIdentifier, key -> new Window(Instant.now().getEpochSecond(), 0));
        synchronized (window) {
            long now = Instant.now().getEpochSecond();
            if (now - window.startEpochSecond >= WINDOW_SECONDS) {
                window.startEpochSecond = now;
                window.requestCount = 0;
            }
            window.requestCount++;
            return window.requestCount <= REQUEST_LIMIT;
        }
    }

    /**
     * Get client identifier (IP or user ID)
     * @param request HTTP request
     * @return client identifier string
     */
    private String getClientIdentifier(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class Window {
        private long startEpochSecond;
        private int requestCount;

        private Window(long startEpochSecond, int requestCount) {
            this.startEpochSecond = startEpochSecond;
            this.requestCount = requestCount;
        }
    }
}
