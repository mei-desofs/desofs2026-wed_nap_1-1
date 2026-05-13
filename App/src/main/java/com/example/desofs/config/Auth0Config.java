package com.example.desofs.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Auth0 configuration properties loaded from environment or application.yml
 * To be implemented with Auth0-specific JWT validation and token parsing
 */
@Configuration
public class Auth0Config {
    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}")
    private String issuerUri;

    @Value("${auth0.domain:}")
    private String auth0Domain;

    @Value("${auth0.audience:}")
    private String audience;

    @Value("${auth0.jwk-set-uri:}")
    private String jwkSetUri;

    public String getIssuerUri() { return issuerUri; }
    public String getAuth0Domain() { return auth0Domain; }
    public String getAudience() { return audience; }
    public String getJwkSetUri() { return jwkSetUri; }
}
