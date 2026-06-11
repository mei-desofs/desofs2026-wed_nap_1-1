package com.example.desofs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Builds the {@link RestClient} used by the Auth0 Management API client
 * with explicit connect/read timeouts sourced from
 * {@link Auth0ManagementProperties}.
 */
@Configuration
public class Auth0ManagementClientConfig {

    @Bean
    public RestClient auth0ManagementRestClient(Auth0ManagementProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeoutMs());
        factory.setReadTimeout(properties.getReadTimeoutMs());
        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}
