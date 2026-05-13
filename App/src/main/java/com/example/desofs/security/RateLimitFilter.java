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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Rate-limiting filter using the token-bucket algorithm (Bucket4j).
 * Applies two layers:
 *   1. Per-IP: 300 requests/minute (protects against anonymous flooding)
 *   2. Per-User (auth0Id from JWT): 120 requests/minute (protects against authenticated abuse)
 *
 * Returns HTTP 429 Too Many Requests when limits are exceeded.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);

    private final ConcurrentMap<String, Bucket> ipBuckets = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Bucket> userBuckets = new ConcurrentHashMap<>();

    @Value("${emovieshop.rate-limit.ip.requests-per-minute:300}")
    private int ipRequestsPerMinute;

    @Value("${emovieshop.rate-limit.user.requests-per-minute:120}")
    private int userRequestsPerMinute;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientIp = resolveClientIp(request);

        // Layer 1: Per-IP rate limit
        Bucket ipBucket = ipBuckets.computeIfAbsent(clientIp, k -> createIpBucket());
        if (!ipBucket.tryConsume(1)) {
            logger.warn("Rate limit exceeded for IP: {}", clientIp);
            sendTooManyRequests(response, "Too many requests from this IP. Please try again later.");
            return;
        }

        // Layer 2: Per-User rate limit (only if authenticated)
        String userId = extractUserId(request);
        if (userId != null) {
            Bucket userBucket = userBuckets.computeIfAbsent(userId, k -> createUserBucket());
            if (!userBucket.tryConsume(1)) {
                logger.warn("Rate limit exceeded for user: {}", userId);
                sendTooManyRequests(response, "Too many requests. Please try again later.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private Bucket createIpBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.simple(ipRequestsPerMinute, Duration.ofMinutes(1)))
                .build();
    }

    private Bucket createUserBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.simple(userRequestsPerMinute, Duration.ofMinutes(1)))
                .build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // Take the first IP (original client) to prevent spoofing via appended headers
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String extractUserId(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }

    private void sendTooManyRequests(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"status\":429,\"message\":\"" + message + "\"}"
        );
    }

    //Testing purposes only«

}
