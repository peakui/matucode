package com.peakui.auth.mini;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WechatClientTest {
    private final ObjectMapper json = new ObjectMapper();
    @Test void unionIdIsOptionalAndSessionKeyIsNotExposed() throws Exception {
        var value = WechatClient.decode(json.readTree("{\"openid\":\"verified-openid\",\"session_key\":\"secret\"}"), "app");
        assertNull(value.unionId()); assertEquals("verified-openid", value.openId());
        assertFalse(json.writeValueAsString(value).contains("secret"));
    }
    @Test void rejectsExpiredCodeAndMissingIdentity() throws Exception {
        var failure = json.readTree("{\"errcode\":40029,\"errmsg\":\"invalid code\"}");
        assertThrows(RuntimeException.class, () -> WechatClient.decode(failure, "app"));
        assertThrows(RuntimeException.class, () -> WechatClient.decode(json.createObjectNode(), "app"));
    }
    @Test void disabledIsExplicitAndEnabledRequiresBothCredentials() {
        assertFalse(new WechatClient("", "", false).enabled());
        assertThrows(RuntimeException.class, () -> new WechatClient("", "", false).exchange("code"));
        assertThrows(IllegalStateException.class, () -> new WechatClient("app", "", true));
    }
}
