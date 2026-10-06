package com.peakui.ai.config;

import com.peakui.ai.AiProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
public class AiWebClientConfig {
    @Bean
    public WebClient aiWebClient(AiProperties properties) {
        ConnectionProvider provider = ConnectionProvider.builder("ai-provider")
                .maxConnections(32)
                .maxIdleTime(Duration.ofSeconds(30))
                .build();
        HttpClient client = HttpClient.create(provider)
                .responseTimeout(Duration.ofMillis(properties.getModel().getReadTimeoutMs()))
                .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        properties.getModel().getConnectTimeoutMs());
        return WebClient.builder().clientConnector(new ReactorClientHttpConnector(client)).build();
    }
}
