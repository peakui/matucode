package com.peakui.gateway.config;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import com.peakui.common.security.IdentitySignature;
import com.peakui.gateway.feign.AuthFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** 清除伪造头，转发真实身份；面试接口（含匿名）必须携带身份及客户端 IP 双签名。 */
@Slf4j
@Component
@RefreshScope
@RequiredArgsConstructor
public class LoginUserRelayFilter implements GlobalFilter, Ordered {

    // 管理端路径统一在网关要求管理员角色；对应服务自身不做角色校验。
    private static final List<String> ADMIN_PATHS = List.of(
            "/pay/admin", "/ai/admin", "/search/admin", "/messages/admin", "/admin/info");
    private static final List<String> ADMIN_ROLES = List.of("admin", "role_admin", "super_admin");

    private final ObjectProvider<AuthFeignClient> authFeignClientProvider;
    private final ClientIpResolver clientIpResolver;

    @Value("${security.identity.secret:}")
    private String identitySecret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = GatewaySecurityHeaders.clear(exchange.getRequest());
        final String path = GatewaySecurityHeaders.path(request);
        final boolean preflight = HttpMethod.OPTIONS.equals(request.getMethod());
        boolean interview = GatewaySecurityHeaders.isInterview(path);
        ServerWebExchange cleanExchange = exchange.mutate().request(request).build();
        // 从原始请求解析，避免独立调用本过滤器时提前删除转发头；生产链中复用 IP 过滤器缓存。
        final String clientIp = interview ? clientIpResolver.resolve(exchange) : null;
        final String secret = identitySecret;

        return Mono.fromCallable(() -> resolveRelayUserInfo(request))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(InvalidAuthorizationException.class,
                        error -> reject(cleanExchange, HttpStatus.UNAUTHORIZED, error.getMessage()).then(Mono.empty()))
                .onErrorResume(error -> {
                    log.error("登录身份校验不可用", error);
                    return reject(cleanExchange, HttpStatus.SERVICE_UNAVAILABLE, "登录校验暂不可用，请稍后重试")
                            .then(Mono.empty());
                })
                .flatMap(userInfo -> {
                    exchange.getAttributes().put(GatewaySecurityHeaders.ATTRIBUTE_USER_ID, userInfo.userId());
                    if (!preflight && isAdminPath(path) && !hasAdminRole(userInfo.roles())) {
                        return reject(cleanExchange,
                                "0".equals(userInfo.userId()) ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN,
                                "需要管理员权限").then(Mono.empty());
                    }
                    if (interview && !IdentitySignature.isValidSecret(secret)) {
                        return reject(cleanExchange, HttpStatus.SERVICE_UNAVAILABLE, "面试服务签名配置不可用");
                    }
                    if (!interview && "0".equals(userInfo.userId())) {
                        return chain.filter(cleanExchange);
                    }
                    long timestamp = System.currentTimeMillis();
                    String userId = userInfo.userId();
                    String roles = IdentitySignature.normalizeRoles(userInfo.roles());
                    ServerHttpRequest mutatedRequest = request.mutate().headers(headers -> {
                        // 其它现有模块依赖这两个身份头，即使尚未配置签名密钥也应保留真实登录身份。
                        headers.set(IdentitySignature.HEADER_USER_ID, userId);
                        headers.set(IdentitySignature.HEADER_USER_ROLES, roles);
                        if (IdentitySignature.isValidSecret(secret)) {
                            headers.set(IdentitySignature.HEADER_TIMESTAMP, String.valueOf(timestamp));
                            headers.set(IdentitySignature.HEADER_SIGNATURE,
                                    IdentitySignature.sign(userId, roles, timestamp, secret));
                            if (interview) {
                                headers.set(IdentitySignature.HEADER_CLIENT_IP, clientIp);
                                headers.set(IdentitySignature.HEADER_CLIENT_IP_SIGNATURE,
                                        IdentitySignature.signClientIp(userId, clientIp, timestamp, secret));
                            }
                        }
                    }).build();
                    return chain.filter(cleanExchange.mutate().request(mutatedRequest).build());
                });
    }

    private RelayUserInfo resolveRelayUserInfo(ServerHttpRequest request) {
        List<String> authorizations = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
        if (authorizations == null) {
            // 必须返回非空值；Mono.fromCallable 的 null 会直接结束请求而不调用下游。
            return new RelayUserInfo("0", "");
        }
        if (authorizations.size() != 1 || !StringUtils.hasText(authorizations.get(0))) {
            throw new InvalidAuthorizationException("登录凭证无效，请重新登录");
        }
        String token = authorizations.get(0).trim();
        if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = token.substring(7).trim();
        }
        if (!StringUtils.hasText(token) || token.equalsIgnoreCase("Bearer")
                || token.chars().anyMatch(Character::isWhitespace) || token.contains(",")) {
            throw new InvalidAuthorizationException("登录凭证无效，请重新登录");
        }
        String loginId = resolveLoginId(token);
        return new RelayUserInfo(loginId, getRoleListByUserId(loginId));
    }

    // 包可见，方便独立测试异步过滤链，不 mock 跨线程静态方法。
    String resolveLoginId(String token) {
        var logic = StpUtil.getStpLogic();
        try {
            String raw = logic.getLoginIdNotHandle(token);
            if (NotLoginException.BE_REPLACED.equals(raw)) {
                throw new InvalidAuthorizationException("账号已在其他设备登录，您已被顶下线，请重新登录");
            }
            if (NotLoginException.KICK_OUT.equals(raw)) {
                throw new InvalidAuthorizationException("您已被踢下线，请重新登录");
            }
            Object loginId = logic.getLoginIdByToken(token);
            if (!logic.isValidLoginId(loginId) || "0".equals(String.valueOf(loginId))) {
                throw new InvalidAuthorizationException("登录已失效，请重新登录");
            }
            return String.valueOf(loginId);
        } catch (NotLoginException e) {
            if (NotLoginException.BE_REPLACED.equals(e.getType())) {
                throw new InvalidAuthorizationException("账号已在其他设备登录，您已被顶下线，请重新登录");
            }
            if (NotLoginException.KICK_OUT.equals(e.getType())) {
                throw new InvalidAuthorizationException("您已被踢下线，请重新登录");
            }
            throw new InvalidAuthorizationException("登录凭证无效，请重新登录");
        }
    }

    private String getRoleListByUserId(String loginId) {
        try {
            AuthFeignClient client = authFeignClientProvider.getIfAvailable();
            if (client == null) {
                return "";
            }
            var result = client.getUserRoles(Long.valueOf(loginId));
            AuthFeignClient.UserRolesResponse response = result == null ? null : result.getData();
            List<String> roles = response == null || response.getRoles() == null
                    ? Collections.emptyList() : response.getRoles();
            return String.join(",", roles);
        } catch (Exception e) {
            log.error("获取用户角色失败，按无角色处理，userId={}", loginId, e);
            return "";
        }
    }

    private static boolean isAdminPath(String path) {
        return path != null && ADMIN_PATHS.stream()
                .anyMatch(prefix -> path.equals(prefix) || path.startsWith(prefix + "/"));
    }

    private static boolean hasAdminRole(String roles) {
        if (roles == null || roles.isBlank()) {
            return false;
        }
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .anyMatch(role -> ADMIN_ROLES.stream().anyMatch(admin -> admin.equalsIgnoreCase(role)));
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        // message 均为本类固定文案，不包含 token 或其它不可信输入。
        byte[] body = ("{\"code\":" + status.value() + ",\"message\":\"" + message + "\",\"data\":null}")
                .getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }

    private record RelayUserInfo(String userId, String roles) {
    }

    static final class InvalidAuthorizationException extends RuntimeException {
        InvalidAuthorizationException(String message) {
            super(message);
        }
    }

    @Override
    public int getOrder() {
        return -50;
    }
}
