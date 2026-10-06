package com.peakui.check;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 打卡服务启动类。
 */
@MapperScan("com.peakui.check.mapper")
@EnableFeignClients(basePackages = "com.peakui.check.feign")
@SpringBootApplication
public class CheckApplication {

    public static void main(String[] args) {
        SpringApplication.run(CheckApplication.class, args);
    }
}
