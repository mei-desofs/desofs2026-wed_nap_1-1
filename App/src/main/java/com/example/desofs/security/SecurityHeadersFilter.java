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
 * Servlet filter that adds a standard set of security-related HTTP headers.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Enforce Cross-Origin Resource Sharing (CORS) policy for allowed origins.</li>
 *   <li>Emit a Content-Security-Policy (CSP) header to restrict allowed
 *   script/style/image sources and reduce XSS risk.</li>
 *   <li>Prevent clickjacking via `X-Frame-Options`.</li>
 *   <li>Hardening headers: `X-Content-Type-Options`, `Strict-Transport-Security`,
 *   `X-XSS-Protection`, and `Referrer-Policy`.</li>
 * </ul>
 *
 * <p>Mapping to ASVS checklist (chapter-level):</p>
 * <ul>
 *   <li><b>V4 - API and Web Service:</b> Proper HTTP header usage (CSP,
 *   clickjacking protection, content-type sniffing prevention) to reduce
 *   common web attacks and communicate policy to clients.</li>
 *   <li><b>V12 - Secure Communication:</b> `Strict-Transport-Security` helps
 *   enforce HTTPS usage and protect transport-level security.</li>
 *   <li><b>V13 - Configuration:</b> CORS allowed origins and the CSP string
 *   are configurable via properties so environments can tune policies safely.</li>
 *   <li><b>V16 - Security Logging and Error Handling:</b> The filter should be
 *   used alongside monitored logging for misconfiguration or repeated
 *   CORS/CSP violations (logging not implemented here to avoid leaking client data).</li>
 * </ul>
 *
 * <p>Operational notes:</p>
 * <ul>
 *   <li>`allowed-origins` should list trusted origins only; do not enable
 *   wildcard (`*`) in production for endpoints that require credentials.</li>
 *   <li>The default CSP provided here is conservative but must be reviewed and
 *   tuned per-application where inline scripts or third-party resources are used.</li>
 *   <li>`Strict-Transport-Security` should only be enabled when HTTPS is
 *   correctly configured and certificates are valid for the deployment.</li>
 * </ul>
 *
 * <p>See Deliverables/Phase1/ASVSChecklist/ASVSChecklist.md for the broader
 * checklist and Deliverables/Phase1/ASVSChecklist/SecurityHeadersFilter_ASVS_Mapping.md
 * for a per-control mapping and rationale.</p>
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {
    private final List<String> allowedOrigins;
    private final String contentSecurityPolicy;

    public SecurityHeadersFilter(@Value("${security.cors.allowed-origins:}") String allowedOriginsCsv,
                                 @Value("${security.csp:default-src 'self'; frame-ancestors 'none';}") String contentSecurityPolicy) {
        if (allowedOriginsCsv == null || allowedOriginsCsv.isBlank()) {
            this.allowedOrigins = java.util.Collections.emptyList();
        } else {
            this.allowedOrigins = java.util.Arrays.stream(allowedOriginsCsv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        this.contentSecurityPolicy = contentSecurityPolicy;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        addCorsHeaders(request, response);
        addContentSecurityPolicy(response);
        addClickjackingProtection(response);
        response.setHeader("Cross-Origin-Resource-Policy", "same-origin");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        response.setHeader("X-XSS-Protection", "1; mode=block");
        response.setHeader("Referrer-Policy", "no-referrer");
        filterChain.doFilter(request, response);
    }

    /**
     * Add CORS headers to response
     * @param request HTTP request (to check Origin)
     * @param response HTTP response
     */
    private void addCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isBlank() && allowedOrigins.contains(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Vary", "Origin");
            response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Authorization,Content-Type,X-Requested-With");
        }
    }

    /**
     * Add CSP (Content Security Policy) header
     * @param response HTTP response
     */
    private void addContentSecurityPolicy(HttpServletResponse response) {
        response.setHeader("Content-Security-Policy", this.contentSecurityPolicy);
    }

    /**
     * Add clickjacking prevention headers
     * @param response HTTP response
     */
    private void addClickjackingProtection(HttpServletResponse response) {
        response.setHeader("X-Frame-Options", "DENY");
    }
}
