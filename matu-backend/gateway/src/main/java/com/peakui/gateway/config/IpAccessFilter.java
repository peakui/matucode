package com.peakui.gateway.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** 黑名单优先；配置非空白名单时，仅允许匹配的 IP/CIDR。 */
@Component
@RefreshScope
@RequiredArgsConstructor
public class IpAccessFilter implements GlobalFilter, Ordered {
    private final ClientIpResolver clientIpResolver;

    @Value("${security.ip.allowlist:}")
    private String allowlist;

    @Value("${security.ip.blocklist:}")
    private String blocklist;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (GatewaySecurityHeaders.isBlockedInterviewAlias(exchange.getRequest().getURI().getPath())) {
            exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
            return exchange.getResponse().setComplete();
        }
        final boolean denied;
        try {
            String clientIp = clientIpResolver.resolve(exchange);
            var allowed = IpAddressRules.parse(allowlist);
            var blocked = IpAddressRules.parse(blocklist);
            denied = IpAddressRules.matches(blocked, clientIp)
                    || (!allowed.isEmpty() && !IpAddressRules.matches(allowed, clientIp));
        } catch (IllegalArgumentException e) {
            // 无效名单/代理配置或无法确定来源时不放行；异常范围不包括下游。
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }
        if (denied) {
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange.mutate().request(GatewaySecurityHeaders.clear(exchange.getRequest())).build());
    }

    @Override
    public int getOrder() {
        return -200;
    }
}
