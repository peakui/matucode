package com.peakui.common.env;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

/**
 * 在配置数据（Nacos、application.yml、spring.config.import）加载之前，把本地 .env 读进 Environment。
 *
 * 必须早于 ConfigDataEnvironmentPostProcessor 执行：Nacos 的 server-addr / username / password
 * 以及 spring.config.import 里的 group= 都在「导入阶段」解析，此时用 spring.config.import 引入的
 * .env 还没生效，只有这里注入的属性、真实 OS 环境变量和 -D 参数可见。
 *
 * 路径由 -Dmatu.dotenv= 指定，默认为当前工作目录下的 .env。默认路径下文件不存在则静默跳过，
 * 便于服务器上改用真实环境变量；显式指定却不存在则直接启动失败，避免路径写错后悄悄走默认值。
 *
 * 以最低优先级加入，所以真实 OS 环境变量和 -D 参数仍然优先于 .env。
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PATH_PROPERTY = "matu.dotenv";
    private static final String DEFAULT_PATH = ".env";
    private static final String PROPERTY_SOURCE_NAME = "matuDotenv";

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String configured = System.getProperty(PATH_PROPERTY);
        Path file = Paths.get(configured != null ? configured : DEFAULT_PATH);
        if (!Files.isRegularFile(file)) {
            if (configured != null) {
                throw new IllegalStateException(PATH_PROPERTY + " 指向的文件不存在: " + file);
            }
            return;
        }
        if (environment.getPropertySources().contains(PROPERTY_SOURCE_NAME)) {
            return;
        }
        Properties properties = new Properties();
        try (Reader reader = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException ex) {
            throw new IllegalStateException("读取 " + file + " 失败", ex);
        }
        environment.getPropertySources().addLast(new PropertiesPropertySource(PROPERTY_SOURCE_NAME, properties));
    }
}
