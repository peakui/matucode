package com.peakui.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.net.InetSocketAddress;
import java.util.List;

/** IP 名单、限流和下游签名共用同一个已解析 IP。默认只信任 TCP 对端。 */
@Component
@RefreshScope
public class ClientIpResolver {
    private static final String ATTRIBUTE = ClientIpResolver.class.getName() + ".resolved";

    @Value("${security.ip.trusted-proxies:}")
    private String trustedProxies;

    public String resolve(ServerWebExchange exchange) {
        String cached = exchange.getAttribute(ATTRIBUTE);
        if (cached != null) {
            return cached;
        }
        InetSocketAddress remote = exchange.getRequest().getRemoteAddress();
        if (remote == null || remote.getAddress() == null) {
            throw new IllegalArgumentException("无法确定客户端 IP");
        }
        String peer = IpAddressRules.canonical(remote.getAddress().getHostAddress());
        List<IpAddressRules.Network> trusted = IpAddressRules.parse(trustedProxies);
        String result = peer;
        if (IpAddressRules.matches(trusted, peer)) {
            List<String> forwarded = exchange.getRequest().getHeaders().get("X-Forwarded-For");
            if (forwarded != null && !forwarded.isEmpty()) {
                String[] hops = String.join(",", forwarded).split(",", -1);
                if (hops.length > 64) {
                    throw new IllegalArgumentException("代理链过长");
                }
                // 从实际对端向左剥离可信代理，停在首个不可信地址；不能取最左端。
                for (int i = hops.length - 1; i >= 0 && IpAddressRules.matches(trusted, result); i--) {
                    result = IpAddressRules.canonical(hops[i].trim());
                }
            }
            // 不读取 X-Real-IP/Forwarded，避免不同头之间的歧义和降级伪造。
        }
        exchange.getAttributes().put(ATTRIBUTE, result);
        return result;
    }
}
