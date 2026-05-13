package com.example.desofs.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;

@Configuration
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}")
    private String issuerUri;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
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
            );

        if (issuerUri != null && !issuerUri.isBlank()) {
            http.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        }

        return http.build();
    }
}
