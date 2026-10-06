package com.peakui.oj.judge.sandbox.model;

import lombok.Data;

@Data
public class CodeSandboxProperties {
    /**
     * sandbox 类型：example / remote
     */
    private String type = "example";

    /**
     * 远程判题服务基础地址，例如：http://127.0.0.1:9000
     */
    private String baseUrl;

    /**
     * 远程判题接口路径，例如：/api/judge/execute
     */
    private String executePath = "/api/judge/execute";

    /**
     * 连接超时（毫秒）
     */
    private int connectTimeout = 3000;

    /**
     * 读取超时（毫秒）
     */
    private int readTimeout = 20000;

    /**
     * 默认 Authorization Bearer token
     */
    private String token;

    /**
     * 自定义鉴权请求头名称，例如 auth
     */
    private String authHeaderName;

    /**
     * 自定义鉴权请求头值，例如 secretKey
     */
    private String authHeaderValue;
}
