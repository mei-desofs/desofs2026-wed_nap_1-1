package com.example.desofs.security;

import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

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

	@Test
	void anonymousAuthenticationDoesNotTriggerUserRateLimit() throws ServletException, IOException {
		RateLimitFilter filter = new RateLimitFilter();
		setLimits(filter, 10, 1);
		// Anonymous tokens must be ignored by extractUserId(), so the user bucket is never used.
		SecurityContextHolder.getContext().setAuthentication(
				new AnonymousAuthenticationToken("key", "anonymousUser",
						List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

		MockHttpServletRequest first = new MockHttpServletRequest();
		first.setRemoteAddr("10.0.0.10");
		MockHttpServletResponse firstResponse = new MockHttpServletResponse();
		filter.doFilter(first, firstResponse, mock(FilterChain.class));
		assertThat(firstResponse.getStatus()).isEqualTo(200);

		MockHttpServletRequest second = new MockHttpServletRequest();
		second.setRemoteAddr("10.0.0.10");
		MockHttpServletResponse secondResponse = new MockHttpServletResponse();
		filter.doFilter(second, secondResponse, mock(FilterChain.class));
		// User bucket has limit 1 but anonymous auth must not consume it; only IP bucket applies.
		assertThat(secondResponse.getStatus()).isEqualTo(200);
	}

	@Test
	void trustedForwardedHeadersFallBackToRemoteAddrWhenHeaderMissing() throws ServletException, IOException {
		RateLimitFilter filter = new RateLimitFilter();
		setLimits(filter, 1, 10);
		ReflectionTestUtils.setField(filter, "trustForwardedHeaders", true);

		MockHttpServletRequest first = new MockHttpServletRequest();
		first.setRemoteAddr("10.0.0.4");
		// No X-Forwarded-For header at all
		MockHttpServletResponse firstResponse = new MockHttpServletResponse();
		filter.doFilter(first, firstResponse, mock(FilterChain.class));
		assertThat(firstResponse.getStatus()).isEqualTo(200);

		MockHttpServletRequest second = new MockHttpServletRequest();
		second.setRemoteAddr("10.0.0.4");
		// Blank X-Forwarded-For - must also fall back to remoteAddr
		second.addHeader("X-Forwarded-For", "   ");
		MockHttpServletResponse secondResponse = new MockHttpServletResponse();
		filter.doFilter(second, secondResponse, mock(FilterChain.class));
		// Same IP both times, so the second request hits the limit.
		assertThat(secondResponse.getStatus()).isEqualTo(429);
	}

	@Test
	@SuppressWarnings("unchecked")
	void evictStaleBucketsRemovesIpAndUserEntriesPastTtl() throws Exception {
		RateLimitFilter filter = new RateLimitFilter();
		setLimits(filter, 10, 10);

		ConcurrentMap<String, Bucket> ipBuckets =
				(ConcurrentMap<String, Bucket>) ReflectionTestUtils.getField(filter, "ipBuckets");
		ConcurrentMap<String, Bucket> userBuckets =
				(ConcurrentMap<String, Bucket>) ReflectionTestUtils.getField(filter, "userBuckets");
		ConcurrentMap<String, Long> lastAccess =
				(ConcurrentMap<String, Long>) ReflectionTestUtils.getField(filter, "lastAccess");

		// Populate buckets and mark access far enough in the past to be considered stale.
		long stale = System.currentTimeMillis() - java.time.Duration.ofHours(1).toMillis();
		ipBuckets.put("1.2.3.4", mock(Bucket.class));
		userBuckets.put("alice", mock(Bucket.class));
		lastAccess.put("ip:1.2.3.4", stale);
		lastAccess.put("user:alice", stale);

		// Trigger eviction by issuing a normal request from a different IP.
		MockHttpServletRequest req = new MockHttpServletRequest();
		req.setRemoteAddr("10.0.0.99");
		filter.doFilter(req, new MockHttpServletResponse(), mock(FilterChain.class));

		assertThat(ipBuckets).doesNotContainKey("1.2.3.4");
		assertThat(userBuckets).doesNotContainKey("alice");
		assertThat(lastAccess).doesNotContainKeys("ip:1.2.3.4", "user:alice");
	}

}