package com.peakui.gateway.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/** 面试接口入口级固定窗口限流；自首次请求起计时，递增和过期在同一 Lua 脚本中完成。 */
@Component
@RefreshScope
@RequiredArgsConstructor
public class InterviewTrafficFilter implements GlobalFilter, Ordered {
    static final DefaultRedisScript<Long> RATE_SCRIPT = new DefaultRedisScript<>("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 or redis.call('TTL', KEYS[1]) < 0 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return count
            """, Long.class);

    private final StringRedisTemplate redis;
    private final ClientIpResolver clientIpResolver;

    @Value("${security.interview.traffic.enabled:true}")
    private boolean enabled;

    @Value("${security.interview.traffic.limit-per-window:120}")
    private long limitPerWindow;

    @Value("${security.interview.traffic.window-seconds:60}")
    private long windowSeconds;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = GatewaySecurityHeaders.path(exchange.getRequest());
        if (!enabled || !GatewaySecurityHeaders.isInterview(path)) {
            return chain.filter(exchange);
        }
        if (windowSeconds <= 0 || limitPerWindow <= 0) {
            exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            return exchange.getResponse().setComplete();
        }
        String ip;
        try {
            ip = clientIpResolver.resolve(exchange);
        } catch (IllegalArgumentException e) {
            exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            return exchange.getResponse().setComplete();
        }
        String key = "security:traffic:interview:" + ip;
        return Mono.fromCallable(() -> {
                    Long count = redis.execute(RATE_SCRIPT, List.of(key), String.valueOf(windowSeconds));
                    if (count == null || count <= 0) {
                        throw new IllegalStateException("Redis 未返回有效限流计数");
                    }
                    return count <= limitPerWindow;
                })
                .subscribeOn(Schedulers.boundedElastic())
                // 只处理 Redis 调用错误。下游异常必须原样传递，不能误报为限流存储故障。
                .onErrorResume(error -> {
                    exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
                    return exchange.getResponse().setComplete().then(Mono.empty());
                })
                .flatMap(allowed -> {
                    if (allowed) {
                        return chain.filter(exchange);
                    }
                    exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                    exchange.getResponse().getHeaders().set("Retry-After", String.valueOf(windowSeconds));
                    return exchange.getResponse().setComplete();
                });
    }

    @Override
    public int getOrder() {
        return -150;
    }
}
