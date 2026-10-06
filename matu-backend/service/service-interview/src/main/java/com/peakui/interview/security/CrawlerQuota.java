package com.peakui.interview.security;

import com.peakui.interview.exception.InterviewException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** 单个主体的短期/24小时配额与阶梯封禁在同一 Redis Cluster slot 内原子执行。 */
@Component
@RequiredArgsConstructor
public class CrawlerQuota {
    private final StringRedisTemplate redis;
    static final DefaultRedisScript<Long> SCRIPT = new DefaultRedisScript<>("""
        local ban = redis.call('TTL', KEYS[3])
        if ban > 0 then return ban end
        local cost = tonumber(ARGV[1])
        local short = tonumber(redis.call('GET', KEYS[1]) or '0')
        local daily = tonumber(redis.call('GET', KEYS[2]) or '0')
        if short + cost > tonumber(ARGV[2]) or daily + cost > tonumber(ARGV[3]) then
          local strikes = redis.call('INCR', KEYS[4])
          if strikes == 1 then redis.call('EXPIRE', KEYS[4], 86400) end
          local wait = math.max(1, redis.call('TTL', KEYS[1]))
          if daily + cost > tonumber(ARGV[3]) then wait = math.max(wait, redis.call('TTL', KEYS[2])) end
          if strikes >= 3 then
            local duration = math.min(3600, 300 * (strikes - 2))
            redis.call('SET', KEYS[3], '1', 'EX', duration)
            wait = math.max(wait, duration)
          end
          return wait
        end
        local s = redis.call('INCRBY', KEYS[1], cost)
        if s == cost then redis.call('EXPIRE', KEYS[1], ARGV[4]) end
        local d = redis.call('INCRBY', KEYS[2], cost)
        if d == cost then redis.call('EXPIRE', KEYS[2], 86400) end
        return 0
        """, Long.class);

    public void consume(String subject, long cost, long shortLimit, long dailyLimit, long windowSeconds) {
        String prefix = "security:interview:{" + DigestUtils.md5DigestAsHex(subject.getBytes(StandardCharsets.UTF_8)) + "}:";
        Long retry;
        try {
            retry = redis.execute(SCRIPT, List.of(prefix + "short", prefix + "day", prefix + "ban", prefix + "strikes"),
                    Long.toString(Math.max(1, cost)), Long.toString(Math.max(1, shortLimit)),
                    Long.toString(Math.max(1, dailyLimit)), Long.toString(Math.max(1, windowSeconds)));
        } catch (Exception e) {
            throw new InterviewException(503, "访问保护服务暂不可用，请稍后重试");
        }
        if (retry == null) throw new InterviewException(503, "访问保护服务暂不可用，请稍后重试");
        if (retry > 0) throw new InterviewException(429, "访问过于频繁，请稍后再试", retry);
    }
}
