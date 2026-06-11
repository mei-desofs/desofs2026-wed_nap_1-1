package com.example.desofs.config;

import com.example.desofs.domain.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Auth0ManagementProperties Unit Tests")
class Auth0ManagementPropertiesTest {

    @Test
    @DisplayName("getBaseUrl and getTokenUrl are derived from the configured domain")
    void urlsDerivedFromDomain() {
        Auth0ManagementProperties props = new Auth0ManagementProperties();
        props.setDomain("tenant.eu.auth0.com");

        assertThat(props.getBaseUrl()).isEqualTo("https://tenant.eu.auth0.com/api/v2");
        assertThat(props.getTokenUrl()).isEqualTo("https://tenant.eu.auth0.com/oauth/token");
    }

    @Test
    @DisplayName("setRoleIds(null) clears the map without throwing (defensive default)")
    void setRoleIdsNull_resetsToEmpty() {
        Auth0ManagementProperties props = new Auth0ManagementProperties();
        Map<Role, String> seed = new EnumMap<>(Role.class);
        seed.put(Role.ADMIN, "rol_admin");
        props.setRoleIds(seed);
        assertThat(props.getRoleIds()).containsEntry(Role.ADMIN, "rol_admin");

        props.setRoleIds(null);

        assertThat(props.getRoleIds()).isEmpty();
    }

    @Test
    @DisplayName("setRoleIds copies the input map (no aliasing with caller)")
    void setRoleIds_copiesInput() {
        Auth0ManagementProperties props = new Auth0ManagementProperties();
        Map<Role, String> seed = new EnumMap<>(Role.class);
        seed.put(Role.ADMIN, "rol_admin");
        props.setRoleIds(seed);

        seed.put(Role.SUPPORT, "rol_support_added_after");

        assertThat(props.getRoleIds()).containsOnlyKeys(Role.ADMIN);
    }

    @Test
    @DisplayName("roleId returns the configured Auth0 role identifier")
    void roleId_returnsConfiguredId() {
        Auth0ManagementProperties props = new Auth0ManagementProperties();
        Map<Role, String> roleIds = new EnumMap<>(Role.class);
        roleIds.put(Role.ADMIN, "rol_admin");
        props.setRoleIds(roleIds);

        assertThat(props.roleId(Role.ADMIN)).isEqualTo("rol_admin");
    }

    @Test
    @DisplayName("roleId throws IllegalStateException when the role mapping is missing")
    void roleId_missingMapping_throws() {
        Auth0ManagementProperties props = new Auth0ManagementProperties();

        assertThatThrownBy(() -> props.roleId(Role.SUPPORT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SUPPORT");
    }

    @Test
    @DisplayName("roleId throws IllegalStateException when the role mapping is blank")
    void roleId_blankMapping_throws() {
        Auth0ManagementProperties props = new Auth0ManagementProperties();
        Map<Role, String> roleIds = new EnumMap<>(Role.class);
        roleIds.put(Role.CUSTOMER, "   ");
        props.setRoleIds(roleIds);

        assertThatThrownBy(() -> props.roleId(Role.CUSTOMER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CUSTOMER");
    }
}
