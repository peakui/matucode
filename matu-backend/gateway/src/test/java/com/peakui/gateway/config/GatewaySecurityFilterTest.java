package com.peakui.gateway.config;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.peakui.common.security.IdentitySignature;
import com.peakui.gateway.feign.AuthFeignClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GatewaySecurityFilterTest {
    private static final String SECRET = "01234567890123456789012345678901";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private MockServerHttpRequest.BaseBuilder<?> request(String path) {
        return MockServerHttpRequest.get(path).remoteAddress(new InetSocketAddress("203.0.113.8", 43210));
    }

    private ClientIpResolver resolver(String proxies) {
        ClientIpResolver resolver = new ClientIpResolver();
        ReflectionTestUtils.setField(resolver, "trustedProxies", proxies);
        return resolver;
    }

    @SuppressWarnings("unchecked")
    private LoginUserRelayFilter relay(String secret, ClientIpResolver resolver) {
        LoginUserRelayFilter filter = new LoginUserRelayFilter(mock(ObjectProvider.class), resolver);
        ReflectionTestUtils.setField(filter, "identitySecret", secret);
        return filter;
    }

    private InterviewTrafficFilter traffic(StringRedisTemplate redis, ClientIpResolver resolver) {
        InterviewTrafficFilter filter = new InterviewTrafficFilter(redis, resolver);
        ReflectionTestUtils.setField(filter, "enabled", true);
        ReflectionTestUtils.setField(filter, "limitPerWindow", 2L);
        ReflectionTestUtils.setField(filter, "windowSeconds", 60L);
        return filter;
    }

    @Test
    void anonymousInterviewIsForwardedOnceWithBothValidSignatures() {
        for (String path : List.of("/interview/questions", "/service-interview/interview/questions")) {
            var exchange = MockServerWebExchange.from(request(path)
                    .header("X-User-Id", "99").header("X-User-Roles", "admin")
                    .header("X-User-Signature", "fake").header("X-Client-IP", "1.1.1.1")
                    .header("X-Client-IP-Signature", "fake").header("X-Forwarded-For", "1.1.1.1"));
            AtomicInteger calls = new AtomicInteger();
            relay(SECRET, resolver("")).filter(exchange, downstream -> {
                calls.incrementAndGet();
                var headers = downstream.getRequest().getHeaders();
                String timestamp = headers.getFirst(IdentitySignature.HEADER_TIMESTAMP);
                assertEquals("0", headers.getFirst("X-User-Id"));
                assertEquals("", headers.getFirst("X-User-Roles"));
                assertEquals("203.0.113.8", headers.getFirst("X-Client-IP"));
                assertNull(headers.getFirst("X-Forwarded-For"));
                assertTrue(IdentitySignature.verify("0", "", timestamp,
                        headers.getFirst("X-User-Signature"), SECRET, 30000));
                assertTrue(IdentitySignature.verifyClientIp("0", "203.0.113.8", timestamp,
                        headers.getFirst("X-Client-IP-Signature"), SECRET, 30000));
                return Mono.empty();
            }).block(TIMEOUT);
            assertEquals(1, calls.get());
        }
    }

    @Test
    void missingSecretOnlyClosesInterviewAndOtherServicesKeepRealIdentity() {
        var filter = spy(relay("", resolver("")));
        doReturn("42").when(filter).resolveLoginId("valid");
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        filter.filter(MockServerWebExchange.from(request("/posts/1").header("Authorization", "Bearer valid")),
                next -> { forwarded.set(next); return Mono.empty(); }).block(TIMEOUT);
        assertEquals("42", forwarded.get().getRequest().getHeaders().getFirst("X-User-Id"));
        assertNull(forwarded.get().getRequest().getHeaders().getFirst("X-User-Signature"));
        var interview = MockServerWebExchange.from(request("/interview/questions"));
        filter.filter(interview, next -> fail("不能放行未签名面试请求")).block(TIMEOUT);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, interview.getResponse().getStatusCode());
    }

    @Test
    void anonymousOtherServiceContinuesAndClearsForgedHeaders() {
        AtomicInteger calls = new AtomicInteger();
        relay("", resolver("")).filter(MockServerWebExchange.from(request("/posts")
                .header("X-User-Id", "1").header("X-Client-IP", "1.1.1.1")), next -> {
            calls.incrementAndGet();
            assertNull(next.getRequest().getHeaders().getFirst("X-User-Id"));
            assertNull(next.getRequest().getHeaders().getFirst("X-Client-IP"));
            return Mono.empty();
        }).block(TIMEOUT);
        assertEquals(1, calls.get());
    }

    @Test
    void malformedAuthorizationIs401EvenOnPublicPath() {
        for (String value : List.of("", "Bearer", "Bearer ", "Basic bad")) {
            var exchange = MockServerWebExchange.from(request("/auth/login").header("Authorization", value));
            relay(SECRET, resolver("")).filter(exchange, next -> fail("不能将无效凭证降级为匿名")).block(TIMEOUT);
            assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        }
    }

    @Test
    void invalidAndReplacedTokensProduce401WithExplicitReplacementMessage() {
        StpLogic original = StpUtil.getStpLogic();
        StpLogic logic = mock(StpLogic.class);
        StpUtil.setStpLogic(logic);
        try {
            for (String raw : List.of("-2", "-4", "-5")) {
                when(logic.getLoginIdNotHandle("token")).thenReturn(raw);
                var exchange = MockServerWebExchange.from(request("/posts").header("Authorization", "token"));
                relay(SECRET, resolver("")).filter(exchange, next -> fail("无效凭证不能透传")).block(TIMEOUT);
                assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
                if (raw.equals("-4")) {
                    assertTrue(exchange.getResponse().getBodyAsString().block(TIMEOUT).contains("顶下线"));
                }
            }
        } finally {
            StpUtil.setStpLogic(original);
        }
    }

    @Test
    void untrustedPeerCannotSpoofForwardedHeadersAndTrustedChainStopsAtFirstUntrustedHop() {
        var spoofed = MockServerWebExchange.from(request("/interview/questions")
                .header("X-Forwarded-For", "10.0.0.1").header("X-Real-IP", "10.0.0.1"));
        assertEquals("203.0.113.8", resolver("").resolve(spoofed));
        var proxied = MockServerWebExchange.from(MockServerHttpRequest.get("/interview/questions")
                .remoteAddress(new InetSocketAddress("10.0.0.2", 321))
                .header("X-Forwarded-For", "1.1.1.1, 198.51.100.2, 10.0.0.3"));
        assertEquals("198.51.100.2", resolver("10.0.0.0/8").resolve(proxied));
        var malformed = MockServerWebExchange.from(MockServerHttpRequest.get("/interview/questions")
                .remoteAddress(new InetSocketAddress("10.0.0.2", 321)).header("X-Forwarded-For", "localhost"));
        assertThrows(IllegalArgumentException.class, () -> resolver("10.0.0.0/8").resolve(malformed));
    }

    @Test
    void blacklistWinsAndNonemptyWhitelistRestrictsAccess() {
        IpAccessFilter filter = new IpAccessFilter(resolver(""));
        ReflectionTestUtils.setField(filter, "allowlist", "203.0.113.0/24");
        ReflectionTestUtils.setField(filter, "blocklist", "203.0.113.8");
        var exchange = MockServerWebExchange.from(request("/interview/questions"));
        filter.filter(exchange, next -> fail("黑名单优先")).block(TIMEOUT);
        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        ReflectionTestUtils.setField(filter, "blocklist", "");
        ReflectionTestUtils.setField(filter, "allowlist", "10.0.0.0/8");
        var outside = MockServerWebExchange.from(request("/interview/questions"));
        filter.filter(outside, next -> fail("非白名单不能访问")).block(TIMEOUT);
        assertEquals(HttpStatus.FORBIDDEN, outside.getResponse().getStatusCode());
    }

    @Test
    void broadDocsRouteCannotBeUsedAsBusinessAlias() {
        IpAccessFilter filter = new IpAccessFilter(resolver(""));
        for (String path : List.of("/service-interview/interview/questions", "/service-interview/admin")) {
            var exchange = MockServerWebExchange.from(request(path));
            filter.filter(exchange, next -> fail("docs 别名业务必须拒绝")).block(TIMEOUT);
            assertEquals(HttpStatus.NOT_FOUND, exchange.getResponse().getStatusCode());
        }
        AtomicInteger calls = new AtomicInteger();
        filter.filter(MockServerWebExchange.from(request("/service-interview/v3/api-docs")), next -> {
            calls.incrementAndGet(); return Mono.empty();
        }).block(TIMEOUT);
        assertEquals(1, calls.get());
    }

    @Test
    void trafficUsesOneAtomicScriptAndCannotSwallowDownstreamError() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(eq(InterviewTrafficFilter.RATE_SCRIPT), anyList(), eq("60"))).thenReturn(1L);
        IllegalStateException downstreamFailure = new IllegalStateException("downstream");
        var exchange = MockServerWebExchange.from(request("/interview/questions"));
        var filter = traffic(redis, resolver(""));
        assertSame(downstreamFailure, assertThrows(IllegalStateException.class,
                () -> filter.filter(exchange, next -> Mono.error(downstreamFailure)).block(TIMEOUT)));
        assertNull(exchange.getResponse().getStatusCode());
        verify(redis).execute(InterviewTrafficFilter.RATE_SCRIPT, List.of("security:traffic:interview:203.0.113.8"), "60");
        verifyNoMoreInteractions(redis);
    }

    @Test
    void redisFailureNullAndOverLimitFailClosedForBothPaths() {
        for (String path : List.of("/interview/questions", "/service-interview/interview/questions")) {
            for (int mode = 0; mode < 3; mode++) {
                StringRedisTemplate redis = mock(StringRedisTemplate.class);
                var stub = when(redis.execute(eq(InterviewTrafficFilter.RATE_SCRIPT), anyList(), eq("60")));
                if (mode == 0) stub.thenThrow(new IllegalStateException("redis down"));
                else stub.thenReturn(mode == 1 ? null : 3L);
                var exchange = MockServerWebExchange.from(request(path));
                traffic(redis, resolver("")).filter(exchange, next -> fail("必须关闭访问")).block(TIMEOUT);
                assertEquals(mode == 2 ? HttpStatus.TOO_MANY_REQUESTS : HttpStatus.SERVICE_UNAVAILABLE,
                        exchange.getResponse().getStatusCode());
                if (mode == 2) assertEquals("60", exchange.getResponse().getHeaders().getFirst("Retry-After"));
            }
        }
    }

    @Test
    void ipAndRelayFiltersDoNotConsumeDownstreamErrors() {
        IllegalStateException failure = new IllegalStateException("downstream");
        GatewayFilterChain chain = next -> Mono.error(failure);
        assertSame(failure, assertThrows(IllegalStateException.class, () ->
                new IpAccessFilter(resolver("")).filter(MockServerWebExchange.from(request("/posts")), chain).block(TIMEOUT)));
        assertSame(failure, assertThrows(IllegalStateException.class, () ->
                relay(SECRET, resolver("")).filter(MockServerWebExchange.from(request("/interview/questions")), chain).block(TIMEOUT)));
    }
}
