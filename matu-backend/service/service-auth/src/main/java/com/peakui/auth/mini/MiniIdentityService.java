package com.peakui.auth.mini;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.peakui.auth.mapper.*;
import com.peakui.auth.model.entity.*;
import com.peakui.auth.util.PasswordUtil;
import com.peakui.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MiniIdentityService {
    public static final String PROVIDER = "WECHAT_MINI";
    public static final String NO_PASSWORD = "!WECHAT_ONLY!";
    private final ExternalIdentityMapper identities;
    private final UserMapper users;
    private final UserProfileMapper profiles;
    private final UserRoleMapper userRoles;
    private final RoleMapper roles;
    private final TransactionTemplate transactions;

    public ExternalIdentity find(MiniModels.WechatIdentity identity) {
        return identities.selectOne(new LambdaQueryWrapper<ExternalIdentity>().eq(ExternalIdentity::getProvider, PROVIDER)
                .eq(ExternalIdentity::getAppId, identity.appId()).eq(ExternalIdentity::getOpenId, identity.openId()));
    }
    public boolean bound(Long userId, String appId) {
        return identities.selectCount(new LambdaQueryWrapper<ExternalIdentity>().eq(ExternalIdentity::getProvider, PROVIDER)
                .eq(ExternalIdentity::getAppId, appId).eq(ExternalIdentity::getUserId, userId)) > 0;
    }
    public Long complete(MiniModels.WechatIdentity identity, Long existingUserId) {
        try {
            return transactions.execute(status -> {
                User user;
                if (existingUserId != null) {
                    user = identities.lockUser(existingUserId);
                    requireActive(user);
                } else {
                    user = createUser();
                }
                ExternalIdentity existing = find(identity);
                if (existing != null) {
                    if (existingUserId != null && existing.getUserId().equals(existingUserId)) return existingUserId;
                    throw new BusinessException(409, "微信已绑定其他账号，请重新登录，不可自动合并账号");
                }
                if (bound(user.getId(), identity.appId())) throw new BusinessException(409, "平台账号已绑定其他微信");
                ExternalIdentity row = new ExternalIdentity();
                row.setUserId(user.getId()); row.setProvider(PROVIDER); row.setAppId(identity.appId());
                row.setOpenId(identity.openId()); row.setUnionId(identity.unionId()); row.setCreatedAt(LocalDateTime.now());
                identities.insert(row);
                return user.getId();
            });
        } catch (DuplicateKeyException e) {
            // The entire transaction (including a newly-created user) has already rolled back.
            throw new BusinessException(409, "绑定状态已变化，请重新微信登录");
        }
    }
    private User createUser() {
        Role role = roles.selectDefaultUserRole();
        if (role == null || !Objects.equals(role.getStatus(), 1)) throw new BusinessException(503, "默认用户角色未初始化");
        String random = UUID.randomUUID().toString().replace("-", "");
        User user = new User();
        user.setUsername("wx_" + random); user.setNickname("码途同学");
        // Existing schema requires a unique email. .invalid is reserved, never deliver mail here.
        user.setEmail(random + "@wechat.accounts.invalid"); user.setPasswordHash(NO_PASSWORD);
        user.setGender(2); user.setStatus(1); user.setCreatedAt(LocalDateTime.now()); user.setUpdatedAt(LocalDateTime.now());
        users.insert(user);
        UserProfile profile = new UserProfile(); profile.setUserId(user.getId()); profile.setActivityLevel(0L);
        profile.setSchoolVerified(0); profile.setCompanyVerified(0); profile.setTitleVerified(0);
        profile.setIsVip(0); profile.setVipLevel(0); profile.setVipDaysRemaining(0);
        profile.setFollowerCount(0); profile.setFollowingCount(0); profile.setViewCount(0); profiles.insert(profile);
        UserRole relation = new UserRole(); relation.setUserId(user.getId()); relation.setRoleId(role.getId()); relation.setCreatedAt(LocalDateTime.now()); userRoles.insert(relation);
        return user;
    }
    public void unlink(Long userId, String appId, String password) {
        transactions.executeWithoutResult(status -> {
            User user = identities.lockUser(userId); requireActive(user);
            if (!passwordEnabled(user) || !PasswordUtil.matches(password, user.getPasswordHash()))
                throw new BusinessException(400, "无法解绑：请验证平台密码，并保留有效登录方式");
            identities.delete(new LambdaQueryWrapper<ExternalIdentity>().eq(ExternalIdentity::getUserId, userId)
                    .eq(ExternalIdentity::getProvider, PROVIDER).eq(ExternalIdentity::getAppId, appId));
        });
    }
    public User active(Long userId) { User user = users.selectById(userId); requireActive(user); return user; }
    private static void requireActive(User user) {
        if (user == null || !Objects.equals(user.getStatus(), 1) || user.getDeletedAt() != null)
            throw new BusinessException(401, "账号不可用，请重新登录");
    }
    public static boolean passwordEnabled(User user) {
        return user.getPasswordHash() != null && (PasswordUtil.isLegacy(user.getPasswordHash()) || PasswordUtil.isBcrypt(user.getPasswordHash()));
    }
}
