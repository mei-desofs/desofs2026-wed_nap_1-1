package com.example.desofs.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.stereotype.Component;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Security Headers Filter to add security-related HTTP headers
 * Includes CORS, CSP, X-Frame-Options, HSTS, etc.
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private final List<String> allowedOrigins;

    public SecurityHeadersFilter(
            @Value("${emovieshop.cors.allowed-origins:http://localhost:3000}") String allowedOriginsStr) {
        this.allowedOrigins = List.of(allowedOriginsStr.split(","));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        addCorsHeaders(request, response);
        addContentSecurityPolicy(response);
        addClickjackingProtection(response);
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        response.setHeader("X-XSS-Protection", "1; mode=block");
        response.setHeader("Referrer-Policy", "no-referrer");
        filterChain.doFilter(request, response);
    }

    /**
     * Add CORS headers to response with origin validation
     */
    private void addCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        if (origin != null && allowedOrigins.contains(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Authorization,Content-Type,X-Requested-With");
            response.setHeader("Access-Control-Max-Age", "3600");
        }
    }

    /**
     * Add CSP (Content Security Policy) header
     * @param response HTTP response
     */
    private void addContentSecurityPolicy(HttpServletResponse response) {
        response.setHeader("Content-Security-Policy", "default-src 'self'; frame-ancestors 'none';");
    }

    /**
     * Add clickjacking prevention headers
     * @param response HTTP response
     */
    private void addClickjackingProtection(HttpServletResponse response) {
        response.setHeader("X-Frame-Options", "DENY");
    }
}
