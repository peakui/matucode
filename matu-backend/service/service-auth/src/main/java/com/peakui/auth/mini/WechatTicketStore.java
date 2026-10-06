package com.peakui.auth.mini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peakui.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class WechatTicketStore {
    private final StringRedisTemplate redis;
    private final ObjectMapper json;
    private final SecureRandom random = new SecureRandom();
    private String key(String ticket) { return "auth:mini:ticket:" + MiniLoginGuard.digest(ticket); }
    public String create(MiniModels.WechatIdentity identity) {
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        try { redis.opsForValue().set(key(ticket), json.writeValueAsString(identity), Duration.ofMinutes(5)); }
        catch (Exception e) { throw new BusinessException(503, "登录凭证暂不可保存，请重试"); }
        return ticket;
    }
    public MiniModels.WechatIdentity consume(String ticket) {
        try {
            String raw = redis.opsForValue().getAndDelete(key(ticket));
            if (raw == null) throw new BusinessException(400, "绑定凭证已使用或过期，请重新微信登录");
            return json.readValue(raw, MiniModels.WechatIdentity.class);
        } catch (BusinessException e) { throw e; }
        catch (Exception e) { throw new BusinessException(503, "登录凭证服务暂不可用，请重新微信登录"); }
    }
}
