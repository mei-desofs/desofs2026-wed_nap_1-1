package com.example.desofs.config;

import com.example.desofs.security.RateLimitFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
/**
 * Configures Spring Security for JWT-based authentication and role-based authorization.
 */
public class SecurityConfig {

    /**
     * Expected token issuer URI used for JWT validation.
     */
    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    /**
     * Expected JWT audience claim for API access.
     */
    @Value("${spring.security.oauth2.resourceserver.jwt.audiences:emovieshop-api}")
    private String audience;

    /**
     * Filter that enforces request rate limits.
     */
    private final RateLimitFilter rateLimitFilter;

    /**
     * Creates the security configuration with required filters.
     *
     * @param rateLimitFilter filter responsible for rate limiting
     */
    public SecurityConfig(RateLimitFilter rateLimitFilter) {
        this.rateLimitFilter = rateLimitFilter;
    }

    /**
     * Builds the main security filter chain.
     *
     * @param http HttpSecurity builder
     * @return configured security filter chain
     * @throws Exception if building the security chain fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Stateless API, JWT-based
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .contentTypeOptions(opt -> {}) // X-Content-Type-Options: nosniff (default)
                        .frameOptions(frame -> frame.deny())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/movies/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/movies").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/orders/**").hasRole("SUPPORT")
                        .requestMatchers(HttpMethod.POST, "/api/orders").hasRole("CUSTOMER")

                        .requestMatchers(HttpMethod.POST, "/api/refunds").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.PUT, "/api/refunds/**").hasRole("SUPPORT")
                        .requestMatchers(HttpMethod.GET, "/api/refunds/**").hasRole("SUPPORT")

                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        .requestMatchers("api/audit-logs").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                )
                .addFilterAfter(rateLimitFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Creates a JWT decoder with issuer and audience validation.
     *
     * @return configured JWT decoder
     */
    @Bean
    @Profile("!test")
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = JwtDecoders.fromIssuerLocation(issuerUri);

        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(audience);
        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuerUri);

        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator));

        return jwtDecoder;
    }

    /**
     * Creates the JWT authentication converter used by the resource server.
     *
     * @return JWT authentication converter configured to use {@code sub} as principal
     */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setPrincipalClaimName("sub");
        return converter;
    }
}
