package com.peakui.auth.mini;

import jakarta.validation.constraints.*;
import java.util.List;

public final class MiniModels {
    private MiniModels() {}
    public record CodeRequest(@NotBlank @Size(max=256) String code) {}
    public record CompleteRequest(@NotBlank @Size(max=128) String ticket,
            @NotBlank @Pattern(regexp="BIND|CREATE") String action,
            @Size(max=100) String account, @Size(max=256) String password) {}
    public record PasswordRequest(@NotBlank @Size(max=256) String password) {}
    public record BindCodeRequest(@NotBlank @Size(max=256) String code, @NotBlank @Size(max=256) String password) {}
    public record DeleteRequest(@Size(max=256) String password, @NotBlank @Size(max=500) String reason) {}
    public record MiniUser(String userId, String username, String nickname, String avatarUrl,
            List<String> roles, boolean vip, String vipExpiredAt, boolean wechatBound, boolean passwordLoginEnabled) {}
    public record Session(MiniUser user, String authorization, long expiresIn) {}
    public record LoginResult(boolean needsBinding, String ticket, Session session) {}
    public record WechatIdentity(String appId, String openId, String unionId) {}
}
