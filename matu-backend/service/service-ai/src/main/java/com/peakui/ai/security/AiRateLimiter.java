package com.peakui.ai.security;

import com.peakui.ai.AiProperties;
import com.peakui.ai.exception.AiBusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AiRateLimiter {
    private final StringRedisTemplate redis;
    private final AiProperties properties;

    public Lease acquire(String identity) {
        String minuteKey = "ai:rate:" + identity + ":" + (System.currentTimeMillis() / 60000);
        Long count = redis.opsForValue().increment(minuteKey);
        if (count != null && count == 1) {
            redis.expire(minuteKey, Duration.ofMinutes(2));
        }
        if (count != null && count > properties.getSecurity().getRateLimitPerMinute()) {
            throw new AiBusinessException(429, "AI 请求过于频繁，请稍后再试");
        }

        String activeKey = "ai:active:" + identity;
        Long active = redis.opsForValue().increment(activeKey);
        if (active != null && active == 1) {
            redis.expire(activeKey, Duration.ofMinutes(5));
        }
        if (active != null && active > properties.getSecurity().getConcurrentLimit()) {
            redis.opsForValue().decrement(activeKey);
            throw new AiBusinessException(429, "同时执行的 AI 请求已达上限，请稍后再试");
        }
        return new Lease(activeKey, UUID.randomUUID().toString());
    }

    public void release(Lease lease) {
        if (lease != null) {
            Long active = redis.opsForValue().decrement(lease.key());
            if (active != null && active <= 0) {
                redis.delete(lease.key());
            }
        }
    }

    public record Lease(String key, String token) {
    }
}
