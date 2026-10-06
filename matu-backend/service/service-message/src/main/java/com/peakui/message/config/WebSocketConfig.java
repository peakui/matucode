package com.peakui.message.config;

import com.peakui.message.websocket.MessageWebSocketHandler;
import com.peakui.message.websocket.MessageWebSocketHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final MessageWebSocketHandler messageWebSocketHandler;
    private final MessageWebSocketHandshakeInterceptor messageWebSocketHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(messageWebSocketHandler, "/messages/ws")
                .addInterceptors(messageWebSocketHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
