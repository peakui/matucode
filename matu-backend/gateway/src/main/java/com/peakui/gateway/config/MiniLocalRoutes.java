package com.peakui.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;

/** Dedicated local miniapp stack; inactive in the normal gateway profile. */
@Configuration
@Profile("mini-local")
public class MiniLocalRoutes {
    @Bean RouteLocator miniLocalRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("mini-local-auth", r -> r.order(-100).path("/auth/**").uri("http://127.0.0.1:18081"))
                .route("mini-local-course", r -> r.order(-100).path("/courses/**").uri("http://127.0.0.1:18086"))
                .route("mini-local-interview", r -> r.order(-100).path("/interview/**").uri("http://127.0.0.1:18087"))
                .build();
    }
}
