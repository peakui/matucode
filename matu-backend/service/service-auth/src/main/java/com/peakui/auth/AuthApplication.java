package com.peakui.auth;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 认证服务启动类。
 */
@MapperScan("com.peakui.auth.mapper")
@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "Matu Auth API", version = "1.0", description = "登录注册认证接口文档"))
public class AuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
