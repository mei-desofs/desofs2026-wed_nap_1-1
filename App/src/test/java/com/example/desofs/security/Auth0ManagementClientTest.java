package com.example.desofs.security;

import com.example.desofs.config.Auth0ManagementProperties;
import com.example.desofs.domain.Role;
import com.example.desofs.exceptions.Auth0ManagementException;
import com.example.desofs.shared.dtos.UserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@DisplayName("Auth0ManagementClient Unit Tests")
class Auth0ManagementClientTest {

    private static final String DOMAIN = "test-tenant.us.auth0.com";
    private static final String AUDIENCE = "https://" + DOMAIN + "/api/v2/";
    private static final String TOKEN_URL = "https://" + DOMAIN + "/oauth/token";

    private MockRestServiceServer mockServer;
    private Auth0ManagementClient client;

    @BeforeEach
    void setUp() {
        Auth0ManagementProperties props = new Auth0ManagementProperties();
        props.setDomain(DOMAIN);
        props.setAudience(AUDIENCE);
        props.setClientId("client-id");
        props.setClientSecret("client-secret");
        Map<Role, String> roleIds = new EnumMap<>(Role.class);
        roleIds.put(Role.ADMIN, "rol_admin");
        roleIds.put(Role.SUPPORT, "rol_support");
        roleIds.put(Role.CUSTOMER, "rol_customer");
        props.setRoleIds(roleIds);

        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        client = new Auth0ManagementClient(props, restClient);
    }

    private void expectTokenCall() {
        mockServer.expect(requestTo(TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.grant_type").value("client_credentials"))
                .andExpect(jsonPath("$.client_id").value("client-id"))
                .andExpect(jsonPath("$.audience").value(AUDIENCE))
                .andRespond(withSuccess(
                        "{\"access_token\":\"tk-1\",\"expires_in\":3600,\"token_type\":\"Bearer\"}",
                        MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("listUsers parses array response and uses bearer token")
    void listUsers_success() {
        expectTokenCall();
        mockServer.expect(requestTo("https://" + DOMAIN + "/api/v2/users"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer tk-1"))
                .andRespond(withSuccess(
                        "[{\"user_id\":\"auth0|1\",\"email\":\"a@b.c\",\"name\":\"A\"}]",
                        MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("https://" + DOMAIN + "/api/v2/users/auth0%7C1/roles"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer tk-1"))
                .andRespond(withSuccess(
                        "[{\"id\":\"rol_admin\",\"name\":\"ADMIN\"}]",
                        MediaType.APPLICATION_JSON));

        List<UserDTO> users = client.listUsers();

        assertThat(users).hasSize(1);
        assertThat(users.get(0).getUserId()).isEqualTo("auth0|1");
        assertThat(users.get(0).getEmail()).isEqualTo("a@b.c");
        assertThat(users.get(0).getRoles()).containsExactly("ADMIN");
        mockServer.verify();
    }

    @Test
    @DisplayName("assignRole posts the resolved role_id payload")
    void assignRole_success() {
        expectTokenCall();
        mockServer.expect(requestTo("https://" + DOMAIN + "/api/v2/users/auth0%7C1/roles"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer tk-1"))
                .andExpect(jsonPath("$.roles[0]").value("rol_admin"))
                .andRespond(withNoContent());

        client.assignRole("auth0|1", Role.ADMIN);

        mockServer.verify();
    }

    @Test
    @DisplayName("removeRole sends DELETE with body")
    void removeRole_success() {
        expectTokenCall();
        mockServer.expect(requestTo("https://" + DOMAIN + "/api/v2/users/auth0%7C1/roles"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(jsonPath("$.roles[0]").value("rol_support"))
                .andRespond(withNoContent());

        client.removeRole("auth0|1", Role.SUPPORT);

        mockServer.verify();
    }

    @Test
    @DisplayName("invalid userId is rejected before any HTTP call")
    void invalidUserId_rejectedLocally() {
        assertThatThrownBy(() -> client.assignRole("../etc/passwd", Role.ADMIN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> client.assignRole("", Role.ADMIN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> client.assignRole(null, Role.ADMIN))
                .isInstanceOf(IllegalArgumentException.class);

        // No HTTP calls expected
        mockServer.verify();
    }

    @Test
    @DisplayName("Auth0 5xx is wrapped in Auth0ManagementException")
    void auth0ServerError_wrapped() {
        expectTokenCall();
        mockServer.expect(requestTo("https://" + DOMAIN + "/api/v2/users"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.listUsers())
                .isInstanceOf(Auth0ManagementException.class)
                .hasMessageNotContaining("client-secret");
    }

    @Test
    @DisplayName("Auth0 401 invalidates cached token")
    void auth0Unauthorized_invalidatesToken() {
        expectTokenCall();
        mockServer.expect(requestTo("https://" + DOMAIN + "/api/v2/users"))
                .andRespond(withUnauthorizedRequest());

        // Subsequent call must re-fetch the token
        expectTokenCall();
        mockServer.expect(requestTo("https://" + DOMAIN + "/api/v2/users"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.listUsers())
                .isInstanceOf(Auth0ManagementException.class);

        List<UserDTO> users = client.listUsers();
        assertThat(users).isEmpty();
        mockServer.verify();
    }

    @Test
    @DisplayName("token is cached across calls")
    void tokenCached() {
        expectTokenCall(); // only one token call expected
        mockServer.expect(requestTo("https://" + DOMAIN + "/api/v2/users"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("https://" + DOMAIN + "/api/v2/users"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        client.listUsers();
        client.listUsers();

        mockServer.verify();
    }

    @Test
    @DisplayName("missing role mapping fails fast")
    void missingRoleMapping_fails() {
        Auth0ManagementProperties props = new Auth0ManagementProperties();
        props.setDomain(DOMAIN);
        props.setAudience(AUDIENCE);
        props.setClientId("c");
        props.setClientSecret("s");
        // no role ids set
        Auth0ManagementClient bare = new Auth0ManagementClient(props,
                RestClient.builder().build());

        assertThatThrownBy(() -> bare.assignRole("auth0|1", Role.ADMIN))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN");
    }

    @Test
    @DisplayName("invalidateSessions issues DELETE /users/{id}/sessions with bearer token")
    void invalidateSessions_success() {
        expectTokenCall();
        mockServer.expect(requestTo("https://" + DOMAIN + "/api/v2/users/auth0%7C1/sessions"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header("Authorization", "Bearer tk-1"))
                .andRespond(withNoContent());

        client.invalidateSessions("auth0|1");

        mockServer.verify();
    }

    @Test
    @DisplayName("invalidateSessions swallows Auth0 errors (best-effort)")
    void invalidateSessions_swallowsAuth0Errors() {
        expectTokenCall();
        mockServer.expect(requestTo("https://" + DOMAIN + "/api/v2/users/auth0%7C1/sessions"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withServerError());

        // Must NOT throw - the denylist is the authoritative defence.
        client.invalidateSessions("auth0|1");

        mockServer.verify();
    }

    @Test
    @DisplayName("invalidateSessions rejects invalid userId locally")
    void invalidateSessions_rejectsInvalidUserId() {
        assertThatThrownBy(() -> client.invalidateSessions("../etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> client.invalidateSessions(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> client.invalidateSessions(null))
                .isInstanceOf(IllegalArgumentException.class);
        mockServer.verify();
    }

    @Test
    @DisplayName("listUsers returns empty list when Auth0 returns null body")
    void listUsers_nullBody_returnsEmpty() {
        expectTokenCall();
        mockServer.expect(requestTo("https://" + DOMAIN + "/api/v2/users"))
                .andRespond(withSuccess("null", MediaType.APPLICATION_JSON));

        List<UserDTO> users = client.listUsers();

        assertThat(users).isEmpty();
        mockServer.verify();
    }

    @Test
    @DisplayName("listUsers returns empty role list when /roles endpoint returns null body")
    void listUsers_nullRolesBody_returnsEmptyRoles() {
        expectTokenCall();
        mockServer.expect(requestTo("https://" + DOMAIN + "/api/v2/users"))
                .andRespond(withSuccess(
                        "[{\"user_id\":\"auth0|1\",\"email\":\"a@b.c\",\"name\":\"A\"}]",
                        MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("https://" + DOMAIN + "/api/v2/users/auth0%7C1/roles"))
                .andRespond(withSuccess("null", MediaType.APPLICATION_JSON));

        List<UserDTO> users = client.listUsers();

        assertThat(users).hasSize(1);
        assertThat(users.get(0).getRoles()).isEmpty();
        mockServer.verify();
    }

    @Test
    @DisplayName("listUsers swallows /roles errors and returns the user with empty roles")
    void listUsers_rolesEndpointError_returnsEmptyRoles() {
        expectTokenCall();
        mockServer.expect(requestTo("https://" + DOMAIN + "/api/v2/users"))
                .andRespond(withSuccess(
                        "[{\"user_id\":\"auth0|1\",\"email\":\"a@b.c\",\"name\":\"A\"}]",
                        MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("https://" + DOMAIN + "/api/v2/users/auth0%7C1/roles"))
                .andRespond(withServerError());

        List<UserDTO> users = client.listUsers();

        assertThat(users).hasSize(1);
        assertThat(users.get(0).getRoles()).isEmpty();
        mockServer.verify();
    }
}
