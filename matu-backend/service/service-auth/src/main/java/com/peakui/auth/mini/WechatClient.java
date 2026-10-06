package com.peakui.auth.mini;

import com.fasterxml.jackson.databind.JsonNode;
import com.peakui.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class WechatClient {
    private final RestClient client;
    private final String appId;
    private final String secret;
    private final boolean enabled;

    public WechatClient(@Value("${app.wechat-mini.app-id:}") String appId,
                        @Value("${app.wechat-mini.app-secret:}") String secret,
                        @Value("${app.wechat-mini.enabled:false}") boolean enabled) {
        this.appId = appId; this.secret = secret; this.enabled = enabled;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); factory.setReadTimeout(5000);
        client = RestClient.builder().requestFactory(factory).baseUrl("https://api.weixin.qq.com").build();
        if (enabled && (appId.isBlank() || secret.isBlank())) throw new IllegalStateException("微信登录开启时必须配置 AppID 和 AppSecret");
    }

    public boolean enabled() { return enabled; }
    public String appId() { return appId; }

    public MiniModels.WechatIdentity exchange(String code) {
        if (!enabled) throw new BusinessException(503, "微信登录尚未配置，请使用平台账号登录");
        try {
            JsonNode data = client.get().uri(builder -> builder.path("/sns/jscode2session")
                    .queryParam("appid", appId).queryParam("secret", secret)
                    .queryParam("js_code", code).queryParam("grant_type", "authorization_code").build())
                    .retrieve().body(JsonNode.class);
            return decode(data, appId);
        } catch (BusinessException e) { throw e; }
        catch (Exception e) {
            // Never log request URLs or response bodies: both can contain credentials.
            throw new BusinessException(502, "微信身份服务暂不可用，请重新发起登录");
        }
    }

    static MiniModels.WechatIdentity decode(JsonNode data, String appId) {
        if (data == null || data.path("errcode").asInt(0) != 0 || data.path("openid").asText("").isBlank())
            throw new BusinessException(400, "微信登录凭证失效，请重新发起登录");
        String unionId = data.path("unionid").asText("");
        // session_key deliberately never leaves this adapter or enters the ticket.
        return new MiniModels.WechatIdentity(appId, data.path("openid").asText(), unionId.isBlank() ? null : unionId);
    }
}
