package com.peakui.gateway.config;

import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peakui.common.result.ApiResponse;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

/**
 * 网关统一鉴权配置。
 */
@Configuration
public class SaTokenConfigure {

    /**
     * 在网关层统一校验登录态，公共查询接口允许游客访问。
     */
    @Bean
    public HttpMessageConverters feignHttpMessageConverters(ObjectMapper objectMapper) {
        return new HttpMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper));
    }

    @Bean
    public SaReactorFilter saReactorFilter(ObjectMapper objectMapper) {
        return new SaReactorFilter()
                .addInclude("/**")
                .addExclude(
                        "/auth/login",
                        "/auth/mini/login",
                        "/auth/mini/wechat/login",
                        "/auth/mini/wechat/complete",
                        "/auth/mini/capabilities",
                        "/auth/register",
                        "/auth/register/email-code",
                        "/messages/ws",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/service-auth/v3/api-docs",
                        "/service-post/v3/api-docs",
                        "/service-check/v3/api-docs",
                        "/service-check/swagger-ui.html",
                        "/service-check/swagger-ui/**",
                        "/service-qa/v3/api-docs",
                        "/service-qa/swagger-ui.html",
                        "/service-qa/swagger-ui/**",
                        "/service-oj/v3/api-docs",
                        "/service-interview/v3/api-docs",
                        "/service-file/v3/api-docs",
                        "/service-message/v3/api-docs",
                        "/service-pay/v3/api-docs",
                        "/service-ai/v3/api-docs",
                        "/service-ai/swagger-ui.html",
                        "/service-ai/swagger-ui/**",
                        "/pay/alipay/notify",
                        "/posts/*/share",
                        "/checks/*/share",
                        "/qa/questions/*/share",
                        "/error")
                .setAuth(obj -> {
                    String method = cn.dev33.satoken.context.SaHolder.getRequest().getMethod();
                    if (HttpMethod.OPTIONS.name().equalsIgnoreCase(method)) {
                        return;
                    }
                    if (HttpMethod.GET.name().equalsIgnoreCase(method)) {
                        SaRouter.match("/auth/mini/**", StpUtil::checkLogin);
                        SaRouter.match("/messages/**", StpUtil::checkLogin);
                        SaRouter.match("/posts/liked", StpUtil::checkLogin);
                        SaRouter.match("/checks/liked", StpUtil::checkLogin);
                        SaRouter.match("/qa/questions/liked", StpUtil::checkLogin);
                        SaRouter.match("/qa/questions/mine", StpUtil::checkLogin);
                        SaRouter.match("/qa/questions/following", StpUtil::checkLogin);
                        SaRouter.match("/qa/answers/mine", StpUtil::checkLogin);
                        SaRouter.match("/interview/internal/**", StpUtil::checkLogin);
                        SaRouter.match("/interview/questions/*/progress", StpUtil::checkLogin);
                        SaRouter.match("/interview/questions/collected", StpUtil::checkLogin);
                        SaRouter.match("/interview/wrong-questions", StpUtil::checkLogin);
                        SaRouter.match("/interview/mock-interviews", StpUtil::checkLogin);
                        SaRouter.match("/oj/classes/*/members", StpUtil::checkLogin);
                        SaRouter.match("/oj/classes/*/assignments", StpUtil::checkLogin);
                        SaRouter.match("/oj/classes/*/assignments/*", StpUtil::checkLogin);
                        SaRouter.match("/oj/classes/*/assignments/*/ranking", StpUtil::checkLogin);
                        SaRouter.match("/oj/classes/*/assignments/*/submissions", StpUtil::checkLogin);
                        SaRouter.match("/oj/classes/*/discussions", StpUtil::checkLogin);
                        SaRouter.match("/oj/classes/*/ranking", StpUtil::checkLogin);
                        SaRouter.match("/oj/classes/submissions", StpUtil::checkLogin);
                        SaRouter.match("/oj/classes/submissions/*", StpUtil::checkLogin);
                        SaRouter.match("/oj/classes/submissions/*/details", StpUtil::checkLogin);
                        SaRouter.match("/pay/**", StpUtil::checkLogin);
                        SaRouter.match("/ai/tasks/**", StpUtil::checkLogin);
                        SaRouter.match("/ai/mcp/**", StpUtil::checkLogin);
                        SaRouter.match("/ai/admin/**", StpUtil::checkLogin);
                        SaRouter.match("/search/admin/**", StpUtil::checkLogin);
                        // 服务间内部接口不应由网关匿名暴露：这些命名空间只供服务间直连调用。
                        SaRouter.match("/auth/internal/**", StpUtil::checkLogin);
                        SaRouter.match("/courses/internal/**", StpUtil::checkLogin);
                        SaRouter.match("/posts/internal/**", StpUtil::checkLogin);
                        SaRouter.match("/checks/internal/**", StpUtil::checkLogin);
                        SaRouter.match("/qa/internal/**", StpUtil::checkLogin);
                        SaRouter.match("/oj/internal/**", StpUtil::checkLogin);
                        SaRouter.match("/messages/internal/**", StpUtil::checkLogin);
                        SaRouter.match("/ai/internal/**", StpUtil::checkLogin);
                        SaRouter.match("/ai/conversations", StpUtil::checkLogin);
                        SaRouter.match("/ai/conversations/**", StpUtil::checkLogin);
                        SaRouter.match("/admin/info/**", StpUtil::checkLogin);
                        // 题目隐藏测试点：匿名不可读。
                        SaRouter.match("/oj/classes/problems/*/test-cases", StpUtil::checkLogin);
                        return;
                    }
                    StpUtil.checkLogin();
                })
                .setError(e -> {
                    cn.dev33.satoken.context.SaHolder.getResponse().setStatus(401);
                    cn.dev33.satoken.context.SaHolder.getResponse().setHeader("Content-Type", "application/json;charset=UTF-8");
                    String message = e instanceof cn.dev33.satoken.exception.NotLoginException notLogin
                            && cn.dev33.satoken.exception.NotLoginException.BE_REPLACED.equals(notLogin.getType())
                            ? "账号已在其他设备登录，您已被顶下线，请重新登录" : e.getMessage();
                    try {
                        return objectMapper.writeValueAsString(ApiResponse.fail(401, message));
                    } catch (Exception ex) {
                        return "{\"code\":401,\"message\":\"未登录或登录已失效\",\"data\":null}";
                    }
                });
    }
}
