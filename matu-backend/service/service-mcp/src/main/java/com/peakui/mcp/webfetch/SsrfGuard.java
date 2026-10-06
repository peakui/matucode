package com.peakui.mcp.webfetch;

import com.peakui.mcp.McpToolException;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

/**
 * Blocks requests to internal/special addresses before they are sent.
 *
 * <p>Residual risk: the host is validated here, then resolved again by the HTTP
 * client, so a DNS rebinding attacker can in theory win the race. That is
 * accepted for this internal-only tool.
 */
@Component
public class SsrfGuard {

    /** Resolves and validates {@code rawUrl}; returns the parsed URI or throws. */
    public URI validate(String rawUrl) {
        URI uri;
        try {
            uri = new URI(rawUrl);
        } catch (URISyntaxException e) {
            throw new McpToolException("非法URL: " + rawUrl);
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new McpToolException("只允许 http/https 协议");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new McpToolException("URL缺少主机名");
        }
        if (uri.getUserInfo() != null) {
            throw new McpToolException("URL不允许携带认证信息");
        }
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(uri.getHost());
        } catch (UnknownHostException e) {
            throw new McpToolException("无法解析主机: " + uri.getHost());
        }
        for (InetAddress address : addresses) {
            if (isBlocked(address)) {
                throw new McpToolException("目标地址被拒绝: " + uri.getHost());
            }
        }
        return uri;
    }

    private static boolean isBlocked(InetAddress address) {
        if (address.isLoopbackAddress() || address.isAnyLocalAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return true;
        }
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            int a = bytes[0] & 0xFF;
            int b = bytes[1] & 0xFF;
            int c = bytes[2] & 0xFF;
            if (a == 0 || a >= 240) {
                return true;                       // 0.0.0.0/8, 240.0.0.0/4
            }
            if (a == 100 && b >= 64 && b <= 127) {
                return true;                       // 100.64.0.0/10 (CGNAT)
            }
            if (a == 169 && b == 254) {
                return true;                       // 169.254.0.0/16, incl. 169.254.169.254 metadata
            }
            if (a == 192 && b == 0 && c == 0) {
                return true;                       // 192.0.0.0/24
            }
            if (a == 198 && (b == 18 || b == 19)) {
                return true;                       // 198.18.0.0/15
            }
        } else if (bytes.length == 16) {
            int first = bytes[0] & 0xFF;
            if ((first & 0xFE) == 0xFC) {
                return true;                       // fc00::/7 unique-local
            }
        }
        return false;
    }
}
