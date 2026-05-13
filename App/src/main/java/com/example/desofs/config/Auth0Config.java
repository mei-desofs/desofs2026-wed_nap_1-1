package com.example.desofs.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Auth0 configuration properties loaded from environment or application.yml
 * To be implemented with Auth0-specific JWT validation and token parsing
 */
@Configuration
public class Auth0Config {
    /**
     * JWT issuer URI used by Spring Security resource server validation.
     */
    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}")
    private String issuerUri;

    /**
     * Auth0 tenant domain, for example: {@code tenant.eu.auth0.com}.
     */
    @Value("${auth0.domain:}")
    private String auth0Domain;

    /**
     * Expected audience claim for API access tokens.
     */
    @Value("${auth0.audience:}")
    private String audience;

    /**
     * URI of the JWK set used to validate JWT signatures.
     */
    @Value("${auth0.jwk-set-uri:}")
    private String jwkSetUri;

    /**
     * Gets the configured issuer URI.
     *
     * @return issuer URI value or an empty string when not configured
     */
    public String getIssuerUri() { return issuerUri; }

    /**
     * Gets the configured Auth0 domain.
     *
     * @return Auth0 domain value or an empty string when not configured
     */
    public String getAuth0Domain() { return auth0Domain; }

    /**
     * Gets the configured token audience.
     *
     * @return audience value or an empty string when not configured
     */
    public String getAudience() { return audience; }

    /**
     * Gets the configured JWK set URI.
     *
     * @return JWK set URI value or an empty string when not configured
     */
    public String getJwkSetUri() { return jwkSetUri; }
}
