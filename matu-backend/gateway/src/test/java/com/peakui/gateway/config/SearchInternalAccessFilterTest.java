package com.peakui.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class SearchInternalAccessFilterTest {
    private final SearchInternalAccessFilter filter = new SearchInternalAccessFilter();

    @Test
    void internalAiAndSearchEndpointsAreBlockedIncludingServiceAliases() {
        for (String path : List.of("/posts/internal/ai/1/summary-comment",
                "/service-post/posts/internal/ai/1/summary-comment",
                "/search/internal/rebuild", "/posts/internal/search-documents")) {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.post(path));
            filter.filter(exchange, next -> fail("内部接口不能经过公网网关")).block(Duration.ofSeconds(3));
            assertEquals(HttpStatus.NOT_FOUND, exchange.getResponse().getStatusCode());
        }
    }

    @Test
    void ordinaryPostRequestsStillReachTheService() {
        AtomicInteger forwarded = new AtomicInteger();
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/posts/1/comments"));
        filter.filter(exchange, next -> {
            forwarded.incrementAndGet();
            return Mono.empty();
        }).block(Duration.ofSeconds(3));
        assertEquals(1, forwarded.get());
    }
}
