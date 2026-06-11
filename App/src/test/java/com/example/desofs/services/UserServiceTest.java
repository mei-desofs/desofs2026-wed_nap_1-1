package com.example.desofs.services;

import com.example.desofs.domain.Role;
import com.example.desofs.security.IAuth0ManagementClient;
import com.example.desofs.shared.dtos.UserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests (UC8)")
class UserServiceTest {

    private static final String ACTOR = "auth0|admin-1";
    private static final String TARGET = "auth0|user-2";

    @Mock
    private IAuth0ManagementClient auth0;

    @Mock
    private IAuditLogService auditLog;

    @Mock
    private ITokenInvalidationService tokenInvalidation;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void resetMocks() {
        reset(auth0, auditLog, tokenInvalidation);
    }

    @Test
    @DisplayName("getAllUsers delegates to Auth0 client")
    void getAllUsers_delegatesToClient() {
        UserDTO user = new UserDTO("auth0|x", "x@x.com", "X");
        when(auth0.listUsers()).thenReturn(List.of(user));

        List<UserDTO> result = userService.getAllUsers();

        assertThat(result).containsExactly(user);
        verify(auth0).listUsers();
        verifyNoInteractions(auditLog, tokenInvalidation);
    }

    @Test
    @DisplayName("assignRole calls Auth0, logs audit, invalidates tokens, drops Auth0 sessions")
    void assignRole_callsClientAndAudits() {
        userService.assignRole(ACTOR, TARGET, Role.SUPPORT);

        var inOrder = inOrder(auth0, auditLog, tokenInvalidation);
        inOrder.verify(auth0).assignRole(TARGET, Role.SUPPORT);
        inOrder.verify(auditLog).logRoleAssignment(ACTOR, TARGET, Role.SUPPORT);
        inOrder.verify(tokenInvalidation).invalidateTokensFor(TARGET, "ROLE_ASSIGNED:SUPPORT");
        inOrder.verify(auth0).invalidateSessions(TARGET);
    }

    @Test
    @DisplayName("removeRole calls Auth0, logs audit, invalidates tokens, drops Auth0 sessions")
    void removeRole_callsClientAndAudits() {
        userService.removeRole(ACTOR, TARGET, Role.CUSTOMER);

        var inOrder = inOrder(auth0, auditLog, tokenInvalidation);
        inOrder.verify(auth0).removeRole(TARGET, Role.CUSTOMER);
        inOrder.verify(auditLog).logRoleRemoval(ACTOR, TARGET, Role.CUSTOMER);
        inOrder.verify(tokenInvalidation).invalidateTokensFor(TARGET, "ROLE_REMOVED:CUSTOMER");
        inOrder.verify(auth0).invalidateSessions(TARGET);
    }

    @Test
    @DisplayName("audit log and token invalidation are NOT written when Auth0 role call fails")
    void assignRole_doesNotAuditOnFailure() {
        doThrow(new RuntimeException("auth0 down"))
                .when(auth0).assignRole(TARGET, Role.ADMIN);

        assertThatThrownBy(() -> userService.assignRole(ACTOR, TARGET, Role.ADMIN))
                .isInstanceOf(RuntimeException.class);

        verify(auditLog, never()).logRoleAssignment(any(), any(), any());
        verify(tokenInvalidation, never()).invalidateTokensFor(any(), any());
        verify(auth0, never()).invalidateSessions(any());
    }

    @Test
    @DisplayName("self-modification is rejected before reaching Auth0 or denylist")
    void assignRole_rejectsSelfModification() {
        assertThatThrownBy(() -> userService.assignRole(ACTOR, ACTOR, Role.ADMIN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("their own roles");

        verifyNoInteractions(auth0, auditLog, tokenInvalidation);
    }

    @Test
    @DisplayName("removeRole self-modification is rejected before reaching Auth0 or denylist")
    void removeRole_rejectsSelfModification() {
        assertThatThrownBy(() -> userService.removeRole(ACTOR, ACTOR, Role.ADMIN))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(auth0, auditLog, tokenInvalidation);
    }

    @Test
    @DisplayName("blank or null actor / target user id is rejected")
    void blankIds_rejected() {
        assertThatThrownBy(() -> userService.assignRole("", TARGET, Role.ADMIN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> userService.assignRole(ACTOR, "", Role.ADMIN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> userService.assignRole(null, TARGET, Role.ADMIN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> userService.assignRole(ACTOR, null, Role.ADMIN))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(auth0, auditLog, tokenInvalidation);
    }

    @Test
    @DisplayName("null role is rejected")
    void nullRole_rejected() {
        assertThatThrownBy(() -> userService.assignRole(ACTOR, TARGET, null))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(auth0, auditLog, tokenInvalidation);
    }
}
