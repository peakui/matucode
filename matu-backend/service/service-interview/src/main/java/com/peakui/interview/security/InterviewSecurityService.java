package com.peakui.interview.security;

import com.peakui.common.security.IdentitySignature;
import com.peakui.interview.exception.InterviewException;
import com.peakui.interview.feign.AuthFeignClient;
import com.peakui.interview.feign.CheckFeignClient;
import com.peakui.interview.model.entity.InterviewQuestion;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.Locale;
import java.security.MessageDigest;
import java.util.Objects;


/** 所有身份和 IP 来自验签后的网关，不使用客户端转发头或客户端提交的解锁状态。 */
@Service
@RefreshScope
@RequiredArgsConstructor
public class InterviewSecurityService {
    private final HttpServletRequest request;
    private final CrawlerQuota quota;
    private final AuthFeignClient authFeignClient;
    private final CheckFeignClient checkFeignClient;

    @Value("${security.identity.secret:}") private String identitySecret;
    @Value("${security.internal.service-secret:}") private String internalServiceSecret;
    @Value("${security.identity.timestamp-skew-ms:30000}") private long timestampSkewMillis;
    @Value("${security.interview.crawler.anonymous-read-limit:30}") private long anonymousReadLimit;
    @Value("${security.interview.crawler.login-read-limit:180}") private long loginReadLimit;
    @Value("${security.interview.crawler.anonymous-day-limit:200}") private long anonymousDayLimit;
    @Value("${security.interview.crawler.login-day-limit:3000}") private long loginDayLimit;
    @Value("${security.interview.crawler.ip-read-limit:900}") private long ipReadLimit;
    @Value("${security.interview.crawler.ip-day-limit:15000}") private long ipDayLimit;
    @Value("${security.interview.crawler.window-seconds:60}") private long windowSeconds;
    @Value("${security.interview.page.anonymous-size:10}") private long anonymousPageSize;
    @Value("${security.interview.page.login-size:20}") private long loginPageSize;

    public boolean isInternalDashboardRequest() {
        String path = request.getServletPath();
        if (!"/interview/internal/dashboard/stats".equals(path)) return false;
        String actual = request.getHeader("X-Internal-Service-Secret");
        return IdentitySignature.isValidSecret(internalServiceSecret)
                && actual != null
                && MessageDigest.isEqual(internalServiceSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                actual.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    public void verifyIdentity() {
        if (identitySecret == null || identitySecret.length() < 32) {
            throw new InterviewException(503, "身份保护尚未配置");
        }
        String uid = request.getHeader(IdentitySignature.HEADER_USER_ID);
        String roles = request.getHeader(IdentitySignature.HEADER_USER_ROLES);
        String timestamp = request.getHeader(IdentitySignature.HEADER_TIMESTAMP);
        String signature = request.getHeader(IdentitySignature.HEADER_SIGNATURE);
        String ip = request.getHeader("X-Client-IP");
        if (!IdentitySignature.verify(uid, roles, timestamp, signature, identitySecret, timestampSkewMillis)
                || !IdentitySignature.verifyClientIp(uid, ip, timestamp, request.getHeader("X-Client-IP-Signature"), identitySecret, timestampSkewMillis)) {
            throw new InterviewException(401, "请求必须通过可信网关");
        }
        try {
            if (Long.parseLong(uid) < 0 || ("0".equals(uid) && roles != null && !roles.isBlank()))
                throw new NumberFormatException();
        } catch (NumberFormatException e) { throw new InterviewException(401, "登录身份无效"); }
    }

    public Long currentUserIdNullable() {
        verifyIdentity();
        long id = Long.parseLong(request.getHeader(IdentitySignature.HEADER_USER_ID));
        return id == 0 ? null : id;
    }

    public Long requireLogin() {
        Long id = currentUserIdNullable();
        if (id == null) throw new InterviewException(401, "请先登录");
        return id;
    }

    public boolean isAdmin() {
        if (currentUserIdNullable() == null) return false;
        String roles = request.getHeader(IdentitySignature.HEADER_USER_ROLES);
        return roles != null && Arrays.stream(roles.split(",")).map(String::trim)
                .map(role -> role.toUpperCase(Locale.ROOT))
                .anyMatch(role -> "ADMIN".equals(role) || "ROLE_ADMIN".equals(role) || "SUPER_ADMIN".equals(role));
    }

    public void requireAdmin() {
        requireLogin();
        if (!isAdmin()) throw new InterviewException(403, "需要管理员权限");
    }

    /** 配额跨题号/分类/搜索条件共享。IP和账号同时计数，登录也不能绕过IP总配额。 */
    public void checkQuestionRead(String resource, long cost) {
        Long userId = currentUserIdNullable();
        String ip = request.getHeader("X-Client-IP");
        quota.consume("ip:" + ip, cost, ipReadLimit, ipDayLimit, windowSeconds);
        if (userId == null) {
            quota.consume("anonymous:" + ip, cost, anonymousReadLimit, anonymousDayLimit, windowSeconds);
        } else {
            quota.consume("user:" + userId, cost, loginReadLimit, loginDayLimit, windowSeconds);
        }
    }

    public void validatePage(Long pageNum, Long pageSize) {
        boolean admin = isAdmin();
        long size = pageSize == null ? 10 : pageSize;
        long page = pageNum == null ? 1 : pageNum;
        long max = admin ? 100 : currentUserIdNullable() == null ? anonymousPageSize : loginPageSize;
        long maxPage = admin ? 10000 : currentUserIdNullable() == null ? 10 : 100;
        if (size < 1 || size > Math.min(100, Math.max(1, max)) || page < 1 || page > maxPage)
            throw new InterviewException(400, "分页参数超过允许范围，请缩小搜索条件");
    }

    public boolean canViewAnswer(InterviewQuestion question) {
        if (currentUserIdNullable() == null) return false;
        return canViewLockedContent(question.getId(), question.getIsLocked(), question.getUnlockDays());
    }

    public boolean canViewLockedContent(Long questionId, Integer isLocked, Integer unlockDays) {
        if (Objects.equals(isLocked, 0)) {
            return true;
        }
        Long userId = currentUserIdNullable();
        if (userId == null) return false;
        if (isAdmin()) return true;
        // 非0的锁定状态（含非法/null）按受限内容处理。权限仅在当前请求缓存。
        Boolean vip = (Boolean) request.getAttribute("interview.vip");
        try {
            if (vip == null) {
                var response = authFeignClient.getVipStatus(userId);
                if (response == null || !Objects.equals(response.getCode(), 0) || response.getData() == null)
                    throw new IllegalStateException();
                vip = Objects.equals(response.getData().userId(), userId) && Boolean.TRUE.equals(response.getData().valid());
                request.setAttribute("interview.vip", vip);
            }
            if (vip) return true;
            if (unlockDays == null || unlockDays <= 0) return false;
            Integer days = (Integer) request.getAttribute("interview.checkDays");
            if (days == null) {
                var response = checkFeignClient.getDays(userId);
                if (response == null || !Objects.equals(response.getCode(), 0) || response.getData() == null
                        || !Objects.equals(response.getData().userId(), userId)) throw new IllegalStateException();
                days = response.getData().totalDays() == null ? 0 : response.getData().totalDays();
                request.setAttribute("interview.checkDays", days);
            }
            return days >= unlockDays;
        } catch (Exception e) {
            throw new InterviewException(503, "解锁状态暂不可用，请稍后重试");
        }
    }
}
