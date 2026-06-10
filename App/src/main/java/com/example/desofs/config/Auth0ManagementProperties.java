package com.example.desofs.config;

import com.example.desofs.domain.Role;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Configuration for the Auth0 Management API client used by UC8.
 *
 * <p>All values are sourced from environment variables in production. The
 * client credentials must never be committed to source control.</p>
 */
@ConfigurationProperties(prefix = "emovieshop.auth0.management")
public class Auth0ManagementProperties {

    /** Tenant domain, e.g. {@code dev-xxxxx.us.auth0.com}. */
    private String domain = "";

    /** Audience expected when requesting a Management API token. */
    private String audience = "";

    /** Client ID of the Machine-to-Machine application. */
    private String clientId = "";

    /** Client secret of the Machine-to-Machine application. */
    private String clientSecret = "";

    /** HTTP timeouts in milliseconds. */
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 10000;

    /** Renew the cached token this many seconds before its expiration. */
    private int tokenRefreshSkewSeconds = 60;

    /** Mapping between application {@link Role} and Auth0 {@code role_id}. */
    private Map<Role, String> roleIds = new EnumMap<>(Role.class);

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }

    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }

    public int getTokenRefreshSkewSeconds() { return tokenRefreshSkewSeconds; }
    public void setTokenRefreshSkewSeconds(int tokenRefreshSkewSeconds) {
        this.tokenRefreshSkewSeconds = tokenRefreshSkewSeconds;
    }

    public Map<Role, String> getRoleIds() {
        return Collections.unmodifiableMap(roleIds);
    }
    public void setRoleIds(Map<Role, String> roleIds) {
        this.roleIds = roleIds == null ? new EnumMap<>(Role.class) : new EnumMap<>(roleIds);
    }

    /**
     * Returns the Management API base URL, e.g. {@code https://{domain}/api/v2}.
     *
     * @return the base URL with no trailing slash
     */
    public String getBaseUrl() {
        return "https://" + domain + "/api/v2";
    }

    /**
     * Returns the OAuth token endpoint, e.g. {@code https://{domain}/oauth/token}.
     *
     * @return the token endpoint URL
     */
    public String getTokenUrl() {
        return "https://" + domain + "/oauth/token";
    }

    /**
     * Resolves the Auth0 {@code role_id} for a given application role.
     *
     * @param role the application role
     * @return the configured Auth0 role identifier
     * @throws IllegalStateException when the role is not configured
     */
    public String roleId(Role role) {
        String id = roleIds.get(role);
        if (id == null || id.isBlank()) {
            throw new IllegalStateException(
                    "Missing Auth0 role_id mapping for role " + role.name());
        }
        return id;
    }
}
