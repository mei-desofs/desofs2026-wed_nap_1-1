package com.example.desofs.security;

import com.example.desofs.config.Auth0ManagementProperties;
import com.example.desofs.domain.Role;
import com.example.desofs.exceptions.Auth0ManagementException;
import com.example.desofs.shared.dtos.UserDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

/**
 * Auth0 Management API client used by the user/role administration flow.
 *
 * <p>Holds a cached Machine-to-Machine access token, refreshing it
 * proactively before expiration. Only the operations required by UC8 are
 * exposed.</p>
 */
@Component
public class Auth0ManagementClient implements IAuth0ManagementClient {

    private static final Logger log = LoggerFactory.getLogger(Auth0ManagementClient.class);

    /** Auth0 user identifiers like {@code auth0|abc}, {@code google-oauth2|123}. */
    private static final Pattern USER_ID_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._-]+\\|[a-zA-Z0-9._-]+$");

    private final Auth0ManagementProperties properties;
    private final RestClient restClient;
    private final ReentrantLock tokenLock = new ReentrantLock();

    private volatile String cachedToken;
    private volatile Instant cachedTokenExpiresAt = Instant.EPOCH;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring-managed singleton properties bean injected by container")
    public Auth0ManagementClient(Auth0ManagementProperties properties,
                                 RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public List<UserDTO> listUsers() {
        Auth0User[] users = restClient.get()
                .uri(properties.getBaseUrl() + "/users")
                .header("Authorization", "Bearer " + acquireToken())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, resp) -> translateError(resp))
                .body(Auth0User[].class);

        if (users == null) {
            return List.of();
        }

        Map<String, String> roleIdToName = new java.util.HashMap<>();
        for (Map.Entry<Role, String> e : properties.getRoleIds().entrySet()) {
            if (e.getValue() != null && !e.getValue().isBlank()) {
                roleIdToName.put(e.getValue(), e.getKey().name());
            }
        }

        return java.util.Arrays.stream(users)
                .map(u -> {
                    List<String> roleNames = fetchUserRoleNames(u.userId, roleIdToName);
                    return new UserDTO(u.userId, u.email, u.name, roleNames);
                })
                .toList();
    }

    private List<String> fetchUserRoleNames(String userId, Map<String, String> roleIdToName) {
        try {
            Auth0Role[] roles = restClient.get()
                    .uri(uri -> uri.scheme("https")
                            .host(properties.getDomain())
                            .path("/api/v2/users/{id}/roles")
                            .build(userId))
                    .header("Authorization", "Bearer " + acquireToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> translateError(resp))
                    .body(Auth0Role[].class);
            if (roles == null) {
                return List.of();
            }
            return java.util.Arrays.stream(roles)
                    .map(r -> roleIdToName.getOrDefault(r.id, r.name))
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Auth0ManagementException ex) {
            log.warn("Failed to fetch roles for user {}: {}", userId, ex.getMessage());
            return List.of();
        }
    }

    @Override
    public void assignRole(String userId, Role role) {
        validateUserId(userId);
        Objects.requireNonNull(role, "role must not be null");
        String roleId = properties.roleId(role);

        restClient.post()
                .uri(uri -> uri.scheme("https")
                        .host(properties.getDomain())
                        .path("/api/v2/users/{id}/roles")
                        .build(userId))
                .header("Authorization", "Bearer " + acquireToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("roles", List.of(roleId)))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, resp) -> translateError(resp))
                .toBodilessEntity();
    }

    @Override
    public void removeRole(String userId, Role role) {
        validateUserId(userId);
        Objects.requireNonNull(role, "role must not be null");
        String roleId = properties.roleId(role);

        restClient.method(org.springframework.http.HttpMethod.DELETE)
                .uri(uri -> uri.scheme("https")
                        .host(properties.getDomain())
                        .path("/api/v2/users/{id}/roles")
                        .build(userId))
                .header("Authorization", "Bearer " + acquireToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("roles", List.of(roleId)))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, resp) -> translateError(resp))
                .toBodilessEntity();
    }

    @Override
    public void invalidateSessions(String userId) {
        validateUserId(userId);
        try {
            restClient.method(org.springframework.http.HttpMethod.DELETE)
                    .uri(uri -> uri.scheme("https")
                            .host(properties.getDomain())
                            .path("/api/v2/users/{id}/sessions")
                            .build(userId))
                    .header("Authorization", "Bearer " + acquireToken())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> translateError(resp))
                    .toBodilessEntity();
        } catch (Auth0ManagementException ex) {
            // Session invalidation is best-effort: the server-side denylist
            // remains the authoritative defence against stale tokens.
            log.warn("Failed to invalidate Auth0 sessions for user: {}", ex.getMessage());
        }
    }

    /**
     * Returns a Management API access token, refreshing the cached value
     * if it is missing or close to expiration.
     *
     * @return a valid bearer token
     */
    String acquireToken() {
        Instant now = Instant.now();
        if (cachedToken != null && now.isBefore(cachedTokenExpiresAt)) {
            return cachedToken;
        }

        tokenLock.lock();
        try {
            if (cachedToken != null && now.isBefore(cachedTokenExpiresAt)) {
                return cachedToken;
            }

            TokenResponse response;
            try {
                response = restClient.post()
                        .uri(properties.getTokenUrl())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .body(Map.of(
                                "grant_type", "client_credentials",
                                "client_id", properties.getClientId(),
                                "client_secret", properties.getClientSecret(),
                                "audience", properties.getAudience()))
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, (req, resp) -> translateError(resp))
                        .body(TokenResponse.class);
            } catch (Auth0ManagementException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new Auth0ManagementException(
                        "Unable to obtain Auth0 Management API token", ex);
            }

            if (response == null || response.accessToken == null || response.accessToken.isBlank()) {
                throw new Auth0ManagementException(
                        "Auth0 token endpoint returned an empty access token");
            }

            int ttl = response.expiresIn > 0 ? response.expiresIn : 86400;
            int skew = Math.max(properties.getTokenRefreshSkewSeconds(), 0);
            int effective = Math.max(ttl - skew, 30);

            cachedToken = response.accessToken;
            cachedTokenExpiresAt = Instant.now().plus(Duration.ofSeconds(effective));
            log.debug("Refreshed Auth0 management token; valid for {} seconds", effective);
            return cachedToken;
        } finally {
            tokenLock.unlock();
        }
    }

    /**
     * Translates an Auth0 HTTP error into a sanitized
     * {@link Auth0ManagementException}, without leaking response payloads.
     */
    private void translateError(org.springframework.http.client.ClientHttpResponse response) {
        try {
            int status = response.getStatusCode().value();
            if (status == 401 || status == 403) {
                cachedToken = null;
                cachedTokenExpiresAt = Instant.EPOCH;
                throw new Auth0ManagementException(
                        "Auth0 Management API rejected the request (status " + status + ")");
            }
            if (status == 429) {
                throw new Auth0ManagementException(
                        "Auth0 Management API rate-limited the request");
            }
            throw new Auth0ManagementException(
                    "Auth0 Management API call failed (status " + status + ")");
        } catch (Auth0ManagementException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new Auth0ManagementException(
                    "Auth0 Management API call failed", ex);
        }
    }

    private static void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must be provided");
        }
        if (userId.length() > 200 || !USER_ID_PATTERN.matcher(userId).matches()) {
            throw new IllegalArgumentException("Invalid userId format");
        }
    }

    /**
     * Auth0 user representation for response deserialization.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class Auth0User {
        @JsonProperty("user_id") String userId;
        @JsonProperty("email") String email;
        @JsonProperty("name") String name;

        UserDTO toDTO() {
            return new UserDTO(userId, email, name);
        }
    }

    /**
     * Subset of the Auth0 token endpoint response.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class TokenResponse {
        @JsonProperty("access_token") String accessToken;
        @JsonProperty("expires_in") int expiresIn;
    }

    /** Subset of Auth0 role payload (id + name). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class Auth0Role {
        @JsonProperty("id") String id;
        @JsonProperty("name") String name;
    }
}
