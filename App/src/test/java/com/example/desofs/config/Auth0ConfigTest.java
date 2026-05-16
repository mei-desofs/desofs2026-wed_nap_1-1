package com.example.desofs.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Auth0Config.class, properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://tenant.eu.auth0.com/",
        "auth0.domain=tenant.eu.auth0.com",
        "auth0.audience=https://emovieshop-api",
        "auth0.jwk-set-uri=https://tenant.eu.auth0.com/.well-known/jwks.json"
})
@DisplayName("Auth0Config Tests")
class Auth0ConfigTest {

    @Autowired
    private Auth0Config auth0Config;

    @Test
    @DisplayName("Auth0Config should expose all configured properties")
    void exposesConfiguredProperties() {
        assertThat(auth0Config.getIssuerUri()).isEqualTo("https://tenant.eu.auth0.com/");
        assertThat(auth0Config.getAuth0Domain()).isEqualTo("tenant.eu.auth0.com");
        assertThat(auth0Config.getAudience()).isEqualTo("https://emovieshop-api");
        assertThat(auth0Config.getJwkSetUri()).isEqualTo("https://tenant.eu.auth0.com/.well-known/jwks.json");
    }
}