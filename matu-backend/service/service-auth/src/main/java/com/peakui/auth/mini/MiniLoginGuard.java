package com.peakui.auth.mini;

import com.peakui.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MiniLoginGuard {
    private final StringRedisTemplate redis;
    private static final DefaultRedisScript<Long> LIMIT = new DefaultRedisScript<>(
            "local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('EXPIRE',KEYS[1],ARGV[1]) end; return n", Long.class);
    public void check(String ip, String account) {
        limit("ip:" + digest(ip), 60, 60);
        if (account != null && !account.isBlank()) limit("account:" + digest(account.trim().toLowerCase(java.util.Locale.ROOT)), 20, 300);
    }
    private void limit(String suffix, int max, int seconds) {
        Long count;
        try { count = redis.execute(LIMIT, List.of("auth:mini:limit:" + suffix), Integer.toString(seconds)); }
        catch (Exception e) { throw new BusinessException(503, "登录保护服务暂不可用"); }
        if (count == null) throw new BusinessException(503, "登录保护服务暂不可用");
        if (count > max) throw new BusinessException(429, "登录尝试过于频繁，请稍后再试");
    }
    static String digest(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException("SHA-256 unavailable"); }
    }
}
