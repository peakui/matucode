package com.peakui.interview.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI interviewOpenApi() {
        return new OpenAPI().info(new Info()
                .title("面试服务接口文档")
                .version("1.0.0")
                .description("service-interview 接口文档"));
    }
}
