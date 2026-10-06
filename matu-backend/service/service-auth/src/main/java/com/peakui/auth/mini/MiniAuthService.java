package com.peakui.auth.mini;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.peakui.auth.mapper.*;
import com.peakui.auth.model.entity.*;
import com.peakui.auth.service.AuthService;
import com.peakui.auth.util.PasswordUtil;
import com.peakui.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MiniAuthService {
    private final AuthService auth;
    private final MiniIdentityService identities;
    private final WechatClient wechat;
    private final WechatTicketStore tickets;
    private final UserProfileMapper profiles;
    private final RoleMapper roles;
    private final ExternalIdentityMapper identityMapper;

    public MiniModels.Session passwordLogin(String account, String password) {
        return issue(auth.authenticate(account, password).getId());
    }
    public MiniModels.LoginResult wechatLogin(String code) {
        var identity = wechat.exchange(code);
        ExternalIdentity existing = identities.find(identity);
        if (existing == null) return new MiniModels.LoginResult(true, tickets.create(identity), null);
        return new MiniModels.LoginResult(false, null, issue(existing.getUserId()));
    }
    public MiniModels.Session complete(MiniModels.CompleteRequest request) {
        if (!wechat.enabled()) throw new BusinessException(503, "微信登录暂不可用");
        // Validate the platform password before consuming the ticket, so a typo can be corrected.
        Long userId = null;
        if ("BIND".equals(request.action())) {
            if (request.account() == null || request.account().isBlank() || request.password() == null || request.password().isBlank())
                throw new BusinessException(400, "请输入原账号和密码");
            userId = auth.authenticate(request.account().trim(), request.password()).getId();
        } else if (!"CREATE".equals(request.action())) throw new BusinessException(400, "未知账号操作");
        var identity = tickets.consume(request.ticket());
        if (!wechat.appId().equals(identity.appId())) throw new BusinessException(400, "小程序配置已变化，请重新登录");
        Long completed = identities.complete(identity, userId);
        // Login is issued AFTER the SQL transaction has committed.
        return issue(completed);
    }
    private MiniModels.Session issue(Long userId) {
        MiniModels.MiniUser user = describe(userId);
        var token = auth.loginMiniUser(userId);
        return new MiniModels.Session(user, token.getAuthorization(), token.getTokenTimeout());
    }
    public MiniModels.MiniUser me() { StpUtil.checkLogin(); return describe(StpUtil.getLoginIdAsLong()); }
    public MiniModels.MiniUser bindCurrent(MiniModels.BindCodeRequest request) {
        StpUtil.checkLogin(); Long id = StpUtil.getLoginIdAsLong(); User user = identities.active(id);
        if (!MiniIdentityService.passwordEnabled(user) || !PasswordUtil.matches(request.password(), user.getPasswordHash()))
            throw new BusinessException(400, "请验证当前平台密码");
        identities.complete(wechat.exchange(request.code()), id);
        return describe(id);
    }
    private MiniModels.MiniUser describe(Long userId) {
        User user = identities.active(userId);
        UserProfile profile = profiles.selectOne(new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId));
        LocalDateTime expiry = profile == null ? null : profile.getVipExpiredAt();
        boolean vip = profile != null && Objects.equals(profile.getIsVip(), 1) && expiry != null
                && expiry.isAfter(LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
        String expiryText = expiry == null ? null : expiry.atZone(ZoneId.of("Asia/Shanghai")).toOffsetDateTime().toString();
        return new MiniModels.MiniUser(userId.toString(), user.getUsername(), user.getNickname(), user.getAvatarUrl(),
                roles.selectRoleCodesByUserId(userId), vip, expiryText, identities.bound(userId, wechat.appId()), MiniIdentityService.passwordEnabled(user));
    }
    public void unlink(String password) {
        StpUtil.checkLogin(); Long id = StpUtil.getLoginIdAsLong();
        identities.unlink(id, wechat.appId(), password);
        StpUtil.logout(id);
    }
    public void requestDeletion(MiniModels.DeleteRequest request) {
        StpUtil.checkLogin(); Long id = StpUtil.getLoginIdAsLong(); User user = identities.active(id);
        if (MiniIdentityService.passwordEnabled(user) && !PasswordUtil.matches(request.password(), user.getPasswordHash()))
            throw new BusinessException(400, "请验证当前平台密码");
        identityMapper.requestDeletion(id, request.reason().trim());
    }
}
