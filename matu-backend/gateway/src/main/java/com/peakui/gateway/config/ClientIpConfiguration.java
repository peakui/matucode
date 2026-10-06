package com.peakui.gateway.config;

import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.adapter.ForwardedHeaderTransformer;

/** 保留真实 socket 对端，由 ClientIpResolver 唯一负责判断可信代理。 */
@Configuration
public class ClientIpConfiguration {
    @Bean
    public WebServerFactoryCustomizer<NettyReactiveWebServerFactory> preserveSocketPeer() {
        return factory -> factory.addServerCustomizers(server -> server.forwarded(false));
    }

    @Bean
    public ForwardedHeaderTransformer forwardedHeaderTransformer() {
        return new ForwardedHeaderTransformer() {
            @Override
            public ServerHttpRequest apply(ServerHttpRequest request) {
                // 不让 Spring 在 IP 校验前根据客户端转发头替换 remoteAddress。
                // 原始头留给 ClientIpResolver 解析，随后由 IpAccessFilter 统一删除。
                return request;
            }
        };
    }
}
