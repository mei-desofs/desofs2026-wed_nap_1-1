package com.example.desofs.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RateLimitFilterTest {

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void defaultsToRemoteAddressAndRejectsSpoofedForwardedFor() throws ServletException, IOException {
		RateLimitFilter filter = new RateLimitFilter();
		setLimits(filter, 1, 10);

		MockHttpServletRequest firstRequest = new MockHttpServletRequest();
		firstRequest.setRemoteAddr("10.0.0.1");
		firstRequest.addHeader("X-Forwarded-For", "203.0.113.10");
		MockHttpServletResponse firstResponse = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);

		filter.doFilter(firstRequest, firstResponse, chain);
		assertThat(firstResponse.getStatus()).isEqualTo(200);
		verify(chain).doFilter(firstRequest, firstResponse);

		MockHttpServletRequest secondRequest = new MockHttpServletRequest();
		secondRequest.setRemoteAddr("10.0.0.1");
		secondRequest.addHeader("X-Forwarded-For", "198.51.100.99");
		MockHttpServletResponse secondResponse = new MockHttpServletResponse();

		filter.doFilter(secondRequest, secondResponse, mock(FilterChain.class));

		assertThat(secondResponse.getStatus()).isEqualTo(429);
		assertThat(secondResponse.getHeader("Retry-After")).isEqualTo("60");
		assertThat(secondResponse.getContentAsString()).contains("Too many requests from this IP");
	}

	@Test
	void authenticatedUserIsRateLimitedSeparatelyFromIp() throws ServletException, IOException {
		RateLimitFilter filter = new RateLimitFilter();
		setLimits(filter, 10, 1);
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken("alice", "password", List.of())
		);

		MockHttpServletRequest firstRequest = new MockHttpServletRequest();
		firstRequest.setRemoteAddr("10.0.0.2");
		MockHttpServletResponse firstResponse = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);

		filter.doFilter(firstRequest, firstResponse, chain);
		assertThat(firstResponse.getStatus()).isEqualTo(200);

		MockHttpServletRequest secondRequest = new MockHttpServletRequest();
		secondRequest.setRemoteAddr("10.0.0.2");
		MockHttpServletResponse secondResponse = new MockHttpServletResponse();

		filter.doFilter(secondRequest, secondResponse, mock(FilterChain.class));

		assertThat(secondResponse.getStatus()).isEqualTo(429);
		assertThat(secondResponse.getHeader("Retry-After")).isEqualTo("60");
		assertThat(secondResponse.getContentAsString()).contains("Too many requests.");
	}

	@Test
	void forwardedHeadersCanBeEnabledExplicitly() throws ServletException, IOException {
		RateLimitFilter filter = new RateLimitFilter();
		setLimits(filter, 1, 10);
		ReflectionTestUtils.setField(filter, "trustForwardedHeaders", true);

		MockHttpServletRequest firstRequest = new MockHttpServletRequest();
		firstRequest.setRemoteAddr("10.0.0.3");
		firstRequest.addHeader("X-Forwarded-For", "203.0.113.20");
		MockHttpServletResponse firstResponse = new MockHttpServletResponse();

		filter.doFilter(firstRequest, firstResponse, mock(FilterChain.class));
		assertThat(firstResponse.getStatus()).isEqualTo(200);

		MockHttpServletRequest secondRequest = new MockHttpServletRequest();
		secondRequest.setRemoteAddr("10.0.0.3");
		secondRequest.addHeader("X-Forwarded-For", "198.51.100.21");
		MockHttpServletResponse secondResponse = new MockHttpServletResponse();

		filter.doFilter(secondRequest, secondResponse, mock(FilterChain.class));

		assertThat(secondResponse.getStatus()).isEqualTo(200);
	}

	// Helper method to set private fields of the filter for testing purposes
	private static void setLimits(RateLimitFilter filter, int ipRequestsPerMinute, int userRequestsPerMinute) {
		ReflectionTestUtils.setField(filter, "ipRequestsPerMinute", ipRequestsPerMinute);
		ReflectionTestUtils.setField(filter, "userRequestsPerMinute", userRequestsPerMinute);
		ReflectionTestUtils.setField(filter, "retryAfterSeconds", 60);
	}

}