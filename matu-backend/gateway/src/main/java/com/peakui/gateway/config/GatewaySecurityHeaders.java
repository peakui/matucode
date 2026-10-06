package com.peakui.gateway.config;

import com.peakui.common.security.IdentitySignature;
import org.springframework.http.server.reactive.ServerHttpRequest;

final class GatewaySecurityHeaders {
    /** 已解析的登录用户 id，供最外层的访问日志读取（请求头可能被客户端伪造，不能直接用）。 */
    static final String ATTRIBUTE_USER_ID = GatewaySecurityHeaders.class.getName() + ".userId";

    private GatewaySecurityHeaders() {
    }

    static ServerHttpRequest clear(ServerHttpRequest request) {
        return request.mutate().headers(headers -> {
            headers.remove(IdentitySignature.HEADER_USER_ID);
            headers.remove(IdentitySignature.HEADER_USER_ROLES);
            headers.remove(IdentitySignature.HEADER_TIMESTAMP);
            headers.remove(IdentitySignature.HEADER_SIGNATURE);
            headers.remove(IdentitySignature.HEADER_CLIENT_IP);
            headers.remove(IdentitySignature.HEADER_CLIENT_IP_SIGNATURE);
            headers.remove("Forwarded");
            headers.remove("X-Forwarded-For");
            headers.remove("X-Real-IP");
        }).build();
    }

    static String path(org.springframework.http.server.reactive.ServerHttpRequest request) {
        return request.getURI().getPath();
    }

    static boolean isInterview(String path) {
        return path.equals("/interview") || path.startsWith("/interview/")
                || path.equals("/service-interview/interview")
                || path.startsWith("/service-interview/interview/");
    }

    static boolean isBlockedInterviewAlias(String path) {
        // 原有 docs 路由过宽，仅放行精确 OpenAPI 文档路径；所有别名业务直接拒绝。
        return (path.equals("/service-interview") || path.startsWith("/service-interview/"))
                && !path.equals("/service-interview/v3/api-docs");
    }
}
