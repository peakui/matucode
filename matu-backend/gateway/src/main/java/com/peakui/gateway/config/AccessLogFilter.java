package com.peakui.gateway.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

/** 全局访问日志：记录每个请求的方法、路径、响应状态、耗时、来源 IP 与登录用户。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessLogFilter implements GlobalFilter, Ordered {

    /** 超过该耗时的请求升级为 WARN，便于发现慢接口。 */
    private static final long SLOW_REQUEST_MILLIS = 1000L;

    private final ClientIpResolver clientIpResolver;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long start = System.currentTimeMillis();
        String method = exchange.getRequest().getMethod() == null
                ? "?" : exchange.getRequest().getMethod().name();
        String path = GatewaySecurityHeaders.path(exchange.getRequest());

        return chain.filter(exchange).doFinally(signal -> {
            long cost = System.currentTimeMillis() - start;
            Integer status = exchange.getResponse().getStatusCode() == null
                    ? null : exchange.getResponse().getStatusCode().value();
            Object userId = exchange.getAttribute(GatewaySecurityHeaders.ATTRIBUTE_USER_ID);
            String template = "{} {} status={} cost={}ms ip={} user={}";
            Object[] args = {method, path, status, cost, resolveIp(exchange), userId == null ? "-" : userId};
            if (cost >= SLOW_REQUEST_MILLIS || (status != null && status >= 500)) {
                log.warn(template, args);
            } else {
                log.info(template, args);
            }
        });
    }

    private String resolveIp(ServerWebExchange exchange) {
        try {
            return clientIpResolver.resolve(exchange);
        } catch (RuntimeException e) {
            InetSocketAddress remote = exchange.getRequest().getRemoteAddress();
            return remote == null || remote.getAddress() == null
                    ? "-" : remote.getAddress().getHostAddress();
        }
    }

    @Override
    public int getOrder() {
        // 放在最外层：被 IP 名单/限流/鉴权拦截的请求也能记录。
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
