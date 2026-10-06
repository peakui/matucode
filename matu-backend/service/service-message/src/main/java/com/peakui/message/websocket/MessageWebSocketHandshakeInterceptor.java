package com.peakui.message.websocket;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Slf4j
@Component
public class MessageWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private static final String TOKEN_PREFIX = "Bearer ";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            log.warn("message websocket handshake missing token, uri={}", request.getURI());
            return false;
        }
        try {
            Object loginId = StpUtil.getLoginIdByToken(token);
            Long userId = Long.valueOf(String.valueOf(loginId));
            attributes.put(MessageWebSocketHandler.USER_ID_ATTRIBUTE, userId);
            log.info("message websocket handshake authenticated, userId={}, uri={}", userId, request.getURI());
            return true;
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            log.warn("message websocket handshake auth failed, uri={}", request.getURI(), e);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.warn("message websocket after handshake failed, uri={}", request.getURI(), exception);
        }
    }

    private String resolveToken(ServerHttpRequest request) {
        URI uri = request.getURI();
        String token = UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("token");
        if (!StringUtils.hasText(token)) {
            token = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        }
        if (!StringUtils.hasText(token)) {
            return null;
        }
        token = token.trim();
        if (token.regionMatches(true, 0, TOKEN_PREFIX, 0, TOKEN_PREFIX.length())) {
            token = token.substring(TOKEN_PREFIX.length()).trim();
        }
        return token;
    }
}
