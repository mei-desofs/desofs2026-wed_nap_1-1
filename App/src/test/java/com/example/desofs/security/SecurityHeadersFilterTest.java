package com.example.desofs.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SecurityHeadersFilterTest {

	@Test
	void doFilter_setsSecurityHeaders_andDelegatesToChain() throws ServletException, IOException {
		// allowlist contains example.com for the test
		SecurityHeadersFilter filter = new SecurityHeadersFilter("https://example.com","default-src 'self'; frame-ancestors 'none';");

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);

		filter.doFilter(request, response, chain);

		// CORS - origin not provided in request -> no CORS header set
		assertThat(response.getHeader("Access-Control-Allow-Origin")).isNull();
		assertThat(response.getHeader("Access-Control-Allow-Methods")).isNull();
		assertThat(response.getHeader("Access-Control-Allow-Headers")).isNull();

		// CSP
		assertThat(response.getHeader("Content-Security-Policy")).isEqualTo("default-src 'self'; frame-ancestors 'none';");

		// Clickjacking
		assertThat(response.getHeader("X-Frame-Options")).isEqualTo("DENY");

		// Other headers
		assertThat(response.getHeader("Cross-Origin-Resource-Policy")).isEqualTo("same-origin");
		assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
		assertThat(response.getHeader("Strict-Transport-Security")).contains("max-age=31536000");
		assertThat(response.getHeader("X-XSS-Protection")).isEqualTo("1; mode=block");
		assertThat(response.getHeader("Referrer-Policy")).isEqualTo("no-referrer");

		// chain invoked
		verify(chain).doFilter(request, response);
	}

	@Test
	void corsMethodsContainOptions() throws ServletException, IOException {
		SecurityHeadersFilter filter = new SecurityHeadersFilter("https://example.com","default-src 'self'; frame-ancestors 'none';");
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);
		// when origin is allowed
		request.addHeader("Origin", "https://example.com");
		filter.doFilter(request, response, chain);

		String methods = response.getHeader("Access-Control-Allow-Methods");
		assertThat(methods).isNotNull();
		assertThat(Arrays.asList(methods.split(","))).contains("OPTIONS");

		// and Access-Control-Allow-Origin must be echo of allowed origin
		assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo("https://example.com");
	}

	@Test
	void originNotAllowed_noCorsHeaderSet() throws ServletException, IOException {
		SecurityHeadersFilter filter = new SecurityHeadersFilter("https://example.com","default-src 'self'; frame-ancestors 'none';");
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);

		request.addHeader("Origin", "https://evil.com");
		filter.doFilter(request, response, chain);

		assertThat(response.getHeader("Access-Control-Allow-Origin")).isNull();
	}
}