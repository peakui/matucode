package com.peakui.post.like;

import org.springframework.data.redis.core.script.DefaultRedisScript;

public final class LikeRedisScript {
    private LikeRedisScript() { }

    // ARGV: user, target, eventId, post, epochMillis, expectedVersion (-1 for normal), force.
    // Versions remain decimal strings outside arithmetic (Lua double has a 53-bit limit).
    public static final DefaultRedisScript<String> MUTATE = new DefaultRedisScript<>("""
        if redis.call('HGET', KEYS[2], 'ready') ~= '1' then return 'UNINITIALIZED' end
        local user = ARGV[1]
        local old = redis.call('SISMEMBER', KEYS[1], user)
        local version = redis.call('HGET', KEYS[3], user) or '0'
        if ARGV[6] ~= '-1' and version ~= ARGV[6] then return 'STALE' end
        if tostring(old) == ARGV[2] and ARGV[7] ~= '1' then
          return '0:' .. (redis.call('HGET', KEYS[2], 'count') or '0') .. ':' .. version
        end
        local sequence = tonumber(redis.call('HGET', KEYS[2], 'sequence') or '0')
        if sequence >= 9007199254740990 then return redis.error_reply('version exhausted') end
        local next = redis.call('HINCRBY', KEYS[2], 'sequence', 1)
        if ARGV[2] == '1' then redis.call('SADD', KEYS[1], user)
        else redis.call('SREM', KEYS[1], user) end
        local count = redis.call('SCARD', KEYS[1])
        redis.call('HSET', KEYS[2], 'count', count)
        redis.call('HSET', KEYS[3], user, string.format('%.0f', next))
        local payload = cjson.encode({eventId=ARGV[3],postId=ARGV[4],userId=user,
          liked=ARGV[2]=='1',version=string.format('%.0f',next),occurredAt=ARGV[5]})
        redis.call('HSET', KEYS[4], user, payload)
        redis.call('PUBLISH', 'post:like:invalidate', ARGV[4])
        return '1:' .. tostring(count) .. ':' .. string.format('%.0f',next) .. ':' .. ARGV[3]
        """, String.class);

    public static final DefaultRedisScript<String> SNAPSHOT = new DefaultRedisScript<>("""
        if redis.call('HGET', KEYS[2], 'ready') ~= '1' then return 'UNINITIALIZED' end
        return tostring(redis.call('SISMEMBER', KEYS[1], ARGV[1])) .. ':' ..
          (redis.call('HGET', KEYS[3], ARGV[1]) or '0')
        """, String.class);

    public static final DefaultRedisScript<Long> COMPLETE = new DefaultRedisScript<>("""
        local raw = redis.call('HGET', KEYS[1], ARGV[1])
        if raw and cjson.decode(raw).eventId == ARGV[2] then
          return redis.call('HDEL', KEYS[1], ARGV[1])
        end
        return 0
        """, Long.class);

    public static final DefaultRedisScript<Long> INSTALL = new DefaultRedisScript<>("""
        if redis.call('GET', KEYS[1]) ~= ARGV[1] then return 0 end
        if redis.call('HGET', KEYS[3], 'ready') == '1' then return 1 end
        redis.call('DEL', KEYS[2], KEYS[4])
        if redis.call('EXISTS', KEYS[6]) == 1 then
          redis.call('RENAME', KEYS[6], KEYS[2]); redis.call('PERSIST', KEYS[2])
        end
        if redis.call('EXISTS', KEYS[7]) == 1 then
          redis.call('RENAME', KEYS[7], KEYS[4]); redis.call('PERSIST', KEYS[4])
        end
        redis.call('HSET', KEYS[3], 'ready', '1', 'count', redis.call('SCARD', KEYS[2]), 'sequence', ARGV[2])
        return 1
        """, Long.class);

    public static final DefaultRedisScript<Long> RELEASE_LOCK = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else return 0 end", Long.class);
}
