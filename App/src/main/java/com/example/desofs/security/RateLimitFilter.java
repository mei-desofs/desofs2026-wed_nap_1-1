package com.example.desofs.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final Duration BUCKET_TTL = Duration.ofMinutes(10);

    private final ConcurrentMap<String, Bucket> ipBuckets = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Bucket> userBuckets = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> lastAccess = new ConcurrentHashMap<>();

    @Value("${emovieshop.rate-limit.ip.requests-per-minute:300}")
    private int ipRequestsPerMinute;

    @Value("${emovieshop.rate-limit.user.requests-per-minute:120}")
    private int userRequestsPerMinute;

    @Value("${emovieshop.rate-limit.trust-forwarded-headers:false}")
    private boolean trustForwardedHeaders;

    @Value("${emovieshop.rate-limit.retry-after-seconds:60}")
    private int retryAfterSeconds;

    /**
     * Applies the per-IP and per-user rate limits before continuing the filter chain.
     *
     * @param request current HTTP request
     * @param response current HTTP response
     * @param filterChain next filter in the chain
     * @throws ServletException if the filter chain fails
     * @throws IOException if response writing fails
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        evictStaleBuckets();

        String clientIp = resolveClientIp(request);

        // Layer 1: Per-IP rate limit
        Bucket ipBucket = ipBuckets.computeIfAbsent(clientIp, k -> createIpBucket());
        markAccess(ipKey(clientIp));
        if (!ipBucket.tryConsume(1)) {
            logger.warn("Rate limit exceeded for IP: {}", clientIp);
            sendTooManyRequests(response, "Too many requests from this IP. Please try again later.");
            return;
        }

        // Layer 2: Per-User rate limit (only if authenticated)
        String userId = extractUserId(request);
        if (userId != null) {
            Bucket userBucket = userBuckets.computeIfAbsent(userId, k -> createUserBucket());
            markAccess(userKey(userId));
            if (!userBucket.tryConsume(1)) {
                logger.warn("Rate limit exceeded for user: {}", userId);
                sendTooManyRequests(response, "Too many requests. Please try again later.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Creates a token bucket for IP-based rate limiting.
     *
     * @return bucket configured with the current IP request limit
     */
    private Bucket createIpBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.simple(ipRequestsPerMinute, Duration.ofMinutes(1)))
                .build();
    }

    /**
     * Creates a token bucket for authenticated-user rate limiting.
     *
     * @return bucket configured with the current user request limit
     */
    private Bucket createUserBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.simple(userRequestsPerMinute, Duration.ofMinutes(1)))
                .build();
    }

    /**
     * Resolves the client IP address, preferring the first value in X-Forwarded-For.
     *
     * @param request current HTTP request
     * @return client IP address
     */
    private String resolveClientIp(HttpServletRequest request) {
        if (trustForwardedHeaders) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // Take the first IP (original client) to prevent spoofing via appended headers
                return xForwardedFor.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    /**
     * Extracts the authenticated user identifier from the security context.
     *
     * @param request current HTTP request
     * @return authenticated user identifier, or {@code null} if unauthenticated
     */
    private String extractUserId(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            return authentication.getName();
        }
        return null;
    }

    /**
     * Writes a standard HTTP 429 response body.
     *
     * @param response current HTTP response
     * @param message user-facing throttling message
     * @throws IOException if the response cannot be written
     */
    private void sendTooManyRequests(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.getWriter().write(
                "{\"status\":429,\"message\":\"" + message + "\"}"
        );
    }

    private void markAccess(String key) {
        lastAccess.put(key, Instant.now().toEpochMilli());
    }

    private void evictStaleBuckets() {
        long cutoff = Instant.now().minus(BUCKET_TTL).toEpochMilli();
        lastAccess.entrySet().removeIf(entry -> {
            if (entry.getValue() >= cutoff) {
                return false;
            }

            String key = entry.getKey();
            if (key.startsWith("ip:")) {
                ipBuckets.remove(key.substring(3));
            } else if (key.startsWith("user:")) {
                userBuckets.remove(key.substring(5));
            }
            return true;
        });
    }

    private String ipKey(String clientIp) {
        return "ip:" + clientIp;
    }

    private String userKey(String userId) {
        return "user:" + userId;
    }

}
