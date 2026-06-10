package com.example.desofs.security;

import com.example.desofs.services.ITokenInvalidationService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenFreshnessFilter Unit Tests")
class TokenFreshnessFilterTest {

    private static final String SUBJECT = "auth0|user-1";

    @Mock
    private ITokenInvalidationService invalidationService;

    @InjectMocks
    private TokenFreshnessFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest("GET", "/api/orders");
        response = new MockHttpServletResponse();
        chain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Jwt buildJwt(String subject, Instant issuedAt) {
        return new Jwt(
                "token-value",
                issuedAt,
                issuedAt == null ? null : issuedAt.plusSeconds(3600),
                Map.of("alg", "RS256"),
                Map.of(
                        "sub", subject == null ? "" : subject,
                        "iat", issuedAt == null ? 0L : issuedAt.getEpochSecond()));
    }

    private void authenticate(Jwt jwt) {
        SecurityContextHolder.getContext()
                .setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @Test
    @DisplayName("Unauthenticated requests pass through untouched")
    void noAuthentication_passesThrough() throws Exception {
        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(invalidationService);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Non-JWT authentication is ignored (passes through)")
    void nonJwtAuthentication_passesThrough() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", "pass", List.of()));

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(invalidationService);
    }

    @Test
    @DisplayName("Fresh JWT (iat after cutoff) is allowed through")
    void freshJwt_allowed() throws Exception {
        Instant iat = Instant.now();
        authenticate(buildJwt(SUBJECT, iat));
        when(invalidationService.isTokenInvalidated(SUBJECT, iat)).thenReturn(false);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Invalidated JWT is rejected with 401 and WWW-Authenticate header")
    void staleJwt_rejected() throws Exception {
        Instant iat = Instant.now().minusSeconds(60);
        authenticate(buildJwt(SUBJECT, iat));
        when(invalidationService.isTokenInvalidated(SUBJECT, iat)).thenReturn(true);

        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getHeader("WWW-Authenticate"))
                .contains("Bearer")
                .contains("invalid_token");
        assertThat(response.getContentAsString())
                .contains("invalid_token")
                .contains("reauthenticate");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("JWT without iat claim passes through (cannot be checked)")
    void jwtWithoutIat_passesThrough() throws Exception {
        // Spring's Jwt builder requires issuedAt non-null at instantiation; emulate
        // a token whose iat is unknown by stubbing the filter dependency to never
        // be called. We just assert the filter chains when iat is null.
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(SUBJECT);
        when(jwt.getIssuedAt()).thenReturn(null);
        authenticate(jwt);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(invalidationService);
    }
}
