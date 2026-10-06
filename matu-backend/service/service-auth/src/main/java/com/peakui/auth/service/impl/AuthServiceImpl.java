package com.peakui.auth.service.impl;

import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.peakui.auth.mapper.UserFollowMapper;
import com.peakui.auth.model.entity.UserFollow;
import com.peakui.auth.model.vo.ActiveDeveloperVO;
import com.peakui.auth.model.vo.FollowStatusVO;
import com.peakui.auth.model.vo.UserBriefVO;
import com.peakui.auth.model.vo.UserFollowVO;
import com.peakui.common.result.PageResponse;
import com.peakui.auth.exception.AuthException;
import com.peakui.auth.mapper.CertificationMapper;
import com.peakui.auth.mapper.LoginLogMapper;
import com.peakui.auth.mapper.RoleMapper;
import com.peakui.auth.mapper.UserMapper;
import com.peakui.auth.mapper.UserProfileMapper;
import com.peakui.auth.mapper.UserRoleMapper;
import com.peakui.auth.mapper.VipActivationRecordMapper;
import com.peakui.auth.model.dto.ActivateVipRequest;
import com.peakui.auth.model.dto.LoginRequest;
import com.peakui.auth.model.dto.RegisterRequest;
import com.peakui.auth.model.dto.ReviewCertificationRequest;
import com.peakui.auth.model.dto.SubmitCertificationRequest;
import com.peakui.auth.model.entity.Certification;
import com.peakui.auth.model.entity.LoginLog;
import com.peakui.auth.model.entity.Role;
import com.peakui.auth.model.entity.User;
import com.peakui.auth.model.entity.UserProfile;
import com.peakui.auth.model.entity.UserRole;
import com.peakui.auth.model.entity.VipActivationRecord;
import com.peakui.auth.model.vo.AuthTokenVO;
import com.peakui.auth.model.vo.CertificationVO;
import com.peakui.auth.model.vo.UserProfileVO;
import com.peakui.auth.model.vo.UserRolesVO;
import com.peakui.auth.model.vo.VipStatusVO;
import com.peakui.auth.model.dto.UpdateAvatarRequest;
import com.peakui.auth.model.dto.UpdateCurrentUserRequest;
import com.peakui.auth.service.AuthService;
import com.peakui.auth.util.PasswordUtil;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.Objects;

/**
 * 登录注册业务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private static final String DEFAULT_AVATAR_URL = "https://your-bucket.oss-cn-hangzhou.aliyuncs.com/defaulthead.png";
    private static final String REGISTER_EMAIL_CODE_KEY_PREFIX = "auth:register:email-code:";
    private static final String REGISTER_EMAIL_LIMIT_KEY_PREFIX = "auth:register:email-limit:";
    private static final Long TEACHER_ROLE_ID = 3L;

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final VipActivationRecordMapper vipActivationRecordMapper;
    private final UserProfileMapper userProfileMapper;
    private final LoginLogMapper loginLogMapper;
    private final UserFollowMapper userFollowMapper;
    private final CertificationMapper certificationMapper;
    private final HttpServletRequest request;
    private final StringRedisTemplate stringRedisTemplate;
    private final JavaMailSender javaMailSender;

    @Value("${app.mail.from}")
    private String mailFrom;

    @Value("${app.auth.register-email-code-expire-minutes:5}")
    private long registerEmailCodeExpireMinutes;

    @Value("${app.auth.register-email-code-limit-seconds:60}")
    private long registerEmailCodeLimitSeconds;

    @Value("${MINI_LOGIN_CONCURRENT:false}")
    private boolean miniLoginConcurrent;

    @Override
    public void sendRegisterEmailCode(String email) {
        if (existsByEmail(email)) {
            throw new AuthException("邮箱已存在");
        }

        String limitKey = buildRegisterEmailLimitKey(email);
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(limitKey))) {
            throw new AuthException("验证码发送过于频繁，请稍后再试");
        }

        String code = generateEmailCode();
        sendEmail(email, code);

        try {
            stringRedisTemplate.opsForValue().set(
                buildRegisterEmailCodeKey(email),
                code,
                registerEmailCodeExpireMinutes,
                TimeUnit.MINUTES
            );
            stringRedisTemplate.opsForValue().set(
                limitKey,
                "1",
                registerEmailCodeLimitSeconds,
                TimeUnit.SECONDS
            );
        } catch (Exception e) {
            log.error("注册验证码缓存失败, email={}", email, e);
            throw new AuthException("验证码已发送，但保存失败，请检查 Redis 后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthTokenVO register(RegisterRequest request) {
        validateRegisterRequest(request);

        if (existsByUsername(request.getUsername())) {
            throw new AuthException("用户名已存在");
        }
        if (existsByEmail(request.getEmail())) {
            throw new AuthException("邮箱已存在");
        }

        checkRegisterEmailCode(request.getEmail(), request.getEmailCode());

        String nickname = StringUtils.hasText(request.getNickname())
            ? request.getNickname().trim()
            : generateNickname();

        User user = new User();
        user.setUsername(request.getUsername());
        user.setNickname(nickname);
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPasswordHash(PasswordUtil.encode(request.getPassword()));
        user.setAvatarUrl(DEFAULT_AVATAR_URL);
        user.setGender(0);
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        initUserProfile(user.getId());
        bindDefaultRole(user.getId());
        clearRegisterEmailCode(request.getEmail());

        return doLogin(user, 1, true, null);
    }

    @Override
    public AuthTokenVO login(LoginRequest request) {
        return doLogin(authenticate(request.getAccount(), request.getPassword()), 1, false, null);
    }

    @Override
    public User authenticate(String account, String password) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
            .eq(User::getUsername, account)
            .or()
            .eq(User::getEmail, account)
            .last("limit 1"));

        if (user == null) {
            saveLoginLog(null, 1, 0, "账号不存在");
            throw new AuthException("账号或密码错误");
        }
        if (!PasswordUtil.matches(password, user.getPasswordHash())) {
            saveLoginLog(user.getId(), 1, 0, "密码错误");
            throw new AuthException("账号或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1 || user.getDeletedAt() != null) {
            saveLoginLog(user.getId(), 1, 0, "账号已禁用");
            throw new AuthException("账号不可用");
        }



        if (PasswordUtil.canUpgrade(password, user.getPasswordHash())) {
            String upgraded = PasswordUtil.encode(password);
            userMapper.update(null, new LambdaUpdateWrapper<User>().eq(User::getId, user.getId())
                    .eq(User::getPasswordHash, user.getPasswordHash()).set(User::getPasswordHash, upgraded));
        }
        return user;
    }

    @Override
    public AuthTokenVO loginMiniUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || !Objects.equals(user.getStatus(), 1) || user.getDeletedAt() != null) {
            throw new AuthException("账号不可用");
        }
        return doLogin(user, 1, false, null, "MP_WEIXIN");
    }

    @Override
    public AuthTokenVO currentUser() {
        StpUtil.checkLogin();
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        if (user == null || !Objects.equals(user.getStatus(), 1) || user.getDeletedAt() != null) {
            throw new AuthException("用户不存在");
        }
        List<String> roles = roleMapper.selectRoleCodesByUserId(userId);
        return AuthTokenVO.builder()
            .userId(user.getId())
            .username(user.getUsername())
            .nickname(user.getNickname())
            .email(user.getEmail())
            .phone(user.getPhone())
            .avatarUrl(user.getAvatarUrl())
            .gender(user.getGender())
            .birthday(user.getBirthday())
            .signature(user.getSignature())
            .roles(roles)
            .tokenName(StpUtil.getTokenName())
            .tokenValue(StpUtil.getTokenValue())
            .authorization(buildAuthorization(StpUtil.getTokenValue()))
            .tokenTimeout(StpUtil.getTokenTimeout())
            .lastLoginTime(user.getLastLoginTime())
            .build();
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthTokenVO updateCurrentUser(UpdateCurrentUserRequest request) {
      StpUtil.checkLogin();
      Long userId = StpUtil.getLoginIdAsLong();
      User user = userMapper.selectById(userId);
      if (user == null) {
          throw new AuthException("用户不存在");
      }

      if (StringUtils.hasText(request.getNickname())) {
          user.setNickname(request.getNickname().trim());
      }
      if (StringUtils.hasText(request.getPhone())) {
          user.setPhone(request.getPhone().trim());
      }
      if (request.getGender() != null) {
          user.setGender(request.getGender());
      }
      if (request.getBirthday() != null) {
          user.setBirthday(request.getBirthday());
      }
      if (request.getSignature() != null) {
          user.setSignature(StringUtils.hasText(request.getSignature()) ? request.getSignature().trim() : null);
      }

      user.setUpdatedAt(LocalDateTime.now());
      userMapper.updateById(user);

      List<String> roles = roleMapper.selectRoleCodesByUserId(userId);
      return AuthTokenVO.builder()
          .userId(user.getId())
          .username(user.getUsername())
          .nickname(user.getNickname())
          .email(user.getEmail())
          .phone(user.getPhone())
          .avatarUrl(user.getAvatarUrl())
          .gender(user.getGender())
          .birthday(user.getBirthday())
          .signature(user.getSignature())
          .roles(roles)
          .tokenName(StpUtil.getTokenName())
          .tokenValue(StpUtil.getTokenValue())
          .authorization(buildAuthorization(StpUtil.getTokenValue()))
          .tokenTimeout(StpUtil.getTokenTimeout())
          .lastLoginTime(user.getLastLoginTime())
          .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthTokenVO updateAvatar(UpdateAvatarRequest request) {
        StpUtil.checkLogin();
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new AuthException("用户不存在");
        }

        user.setAvatarUrl(request.getAvatarUrl().trim());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        List<String> roles = roleMapper.selectRoleCodesByUserId(userId);
        return AuthTokenVO.builder()
          .userId(user.getId())
          .username(user.getUsername())
          .nickname(user.getNickname())
          .email(user.getEmail())
          .phone(user.getPhone())
          .avatarUrl(user.getAvatarUrl())
          .gender(user.getGender())
          .birthday(user.getBirthday())
          .signature(user.getSignature())
          .roles(roles)
          .tokenName(StpUtil.getTokenName())
          .tokenValue(StpUtil.getTokenValue())
          .authorization(buildAuthorization(StpUtil.getTokenValue()))
          .tokenTimeout(StpUtil.getTokenTimeout())
          .lastLoginTime(user.getLastLoginTime())
          .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void followUser(Long userId) {
        StpUtil.checkLogin();
        Long currentUserId = StpUtil.getLoginIdAsLong();

        if (Objects.equals(currentUserId, userId)) {
            throw new AuthException("不能关注自己");
        }

        User targetUser = userMapper.selectById(userId);
        if (targetUser == null) {
            throw new AuthException("目标用户不存在");
        }

        UserFollow exist = userFollowMapper.selectOne(
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowerId, currentUserId)
                        .eq(UserFollow::getFollowingId, userId)
                        .last("limit 1")
        );
        if (exist != null) {
            return;
        }

        UserFollow follow = new UserFollow();
        follow.setFollowerId(currentUserId);
        follow.setFollowingId(userId);
        follow.setCreatedAt(LocalDateTime.now());
        userFollowMapper.insert(follow);

        increaseFollowingCount(currentUserId);
        increaseFollowerCount(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfollowUser(Long userId) {
        StpUtil.checkLogin();
        Long currentUserId = StpUtil.getLoginIdAsLong();

        int deleted = userFollowMapper.delete(
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowerId, currentUserId)
                        .eq(UserFollow::getFollowingId, userId)
        );

        if (deleted > 0) {
            decreaseFollowingCount(currentUserId);
            decreaseFollowerCount(userId);
        }
    }

    @Override
    public PageResponse<UserFollowVO> listMyFollowing(Long pageNum, Long pageSize) {
        StpUtil.checkLogin();
        Long currentUserId = StpUtil.getLoginIdAsLong();
        return listFollowingByUserId(currentUserId, currentUserId, pageNum, pageSize);
    }

    @Override
    public PageResponse<UserFollowVO> listMyFollowers(Long pageNum, Long pageSize) {
        StpUtil.checkLogin();
        Long currentUserId = StpUtil.getLoginIdAsLong();
        return listFollowersByUserId(currentUserId, currentUserId, pageNum, pageSize);
    }

    @Override
    public PageResponse<UserFollowVO> listUserFollowing(Long userId, Long pageNum, Long pageSize) {
        Long currentUserId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        getUserOrThrow(userId);
        return listFollowingByUserId(userId, currentUserId, pageNum, pageSize);
    }

    @Override
    public PageResponse<UserFollowVO> listUserFollowers(Long userId, Long pageNum, Long pageSize) {
        Long currentUserId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        getUserOrThrow(userId);
        return listFollowersByUserId(userId, currentUserId, pageNum, pageSize);
    }

    @Override
    public FollowStatusVO getFollowStatus(Long userId) {
        StpUtil.checkLogin();
        Long currentUserId = StpUtil.getLoginIdAsLong();

        Long count = userFollowMapper.selectCount(
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowerId, currentUserId)
                        .eq(UserFollow::getFollowingId, userId)
        );

        return FollowStatusVO.builder()
                .isFollowing(count != null && count > 0)
                .build();
    }

    @Override
    public List<ActiveDeveloperVO> listActiveDevelopers() {
        List<UserProfile> profiles = userProfileMapper.selectList(
                new LambdaQueryWrapper<UserProfile>()
                        .orderByDesc(UserProfile::getActivityLevel)
                        .last("limit 10")
        );

        return profiles.stream()
                .map(profile -> {
                    User user = userMapper.selectById(profile.getUserId());
                    if (user == null || user.getDeletedAt() != null || user.getStatus() == null || user.getStatus() != 1) {
                        return null;
                    }
                    return ActiveDeveloperVO.builder()
                            .userId(user.getId())
                            .username(user.getUsername())
                            .nickname(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername())
                            .avatarUrl(resolveAvatarUrl(user.getAvatarUrl()))
                            .signature(user.getSignature())
                            .activityLevel(profile.getActivityLevel() == null ? 0L : profile.getActivityLevel())
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public void increaseActivityLevel(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeletedAt() != null || user.getStatus() == null || user.getStatus() != 1) {
            throw new AuthException("用户不存在或状态异常");
        }
        userProfileMapper.update(null, new LambdaUpdateWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId)
                .setSql("activity_level = IFNULL(activity_level, 0) + 1"));
    }

    @Override
    public UserProfileVO getUserProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new AuthException("用户不存在");
        }
        return toUserProfileVO(user);
    }

    @Override
    public List<UserBriefVO> getUserBriefs(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        List<Long> ids = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return userMapper.selectBatchIds(ids).stream()
                .map(user -> UserBriefVO.builder()
                        .userId(user.getId())
                        .username(user.getUsername())
                        .nickname(user.getNickname())
                        .avatarUrl(user.getAvatarUrl())
                        .build())
                .toList();
    }

    @Override
    public UserProfileVO getUserProfileByUsername(String username) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .last("limit 1"));
        if (user == null) {
            throw new AuthException("用户不存在");
        }
        return toUserProfileVO(user);
    }

    @Override
    public UserRolesVO getUserRoles(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new AuthException("用户不存在");
        }
        return UserRolesVO.builder()
            .userId(userId)
            .roles(roleMapper.selectRoleCodesByUserId(userId))
            .build();
    }

    @Override
    public VipStatusVO getVipStatus(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeletedAt() != null || !Objects.equals(user.getStatus(), 1)) {
            throw new AuthException("用户不存在或状态异常");
        }
        UserProfile profile = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId)
                .last("limit 1"));
        return toVipStatus(userId, profile);
    }

    private VipStatusVO toVipStatus(Long userId, UserProfile profile) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiredAt = profile == null ? null : profile.getVipExpiredAt();
        boolean valid = profile != null && Objects.equals(profile.getIsVip(), 1)
                && expiredAt != null && expiredAt.isAfter(now);
        return VipStatusVO.builder()
                .userId(userId)
                .valid(valid)
                .isVip(valid ? 1 : 0)
                .vipLevel(valid && profile.getVipLevel() != null ? profile.getVipLevel() : 0)
                .vipExpiredAt(valid ? expiredAt : null)
                .vipDaysRemaining(valid ? Math.max(0, (int) java.time.Duration.between(now, expiredAt).toDays()) : 0)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activateVip(Long userId, ActivateVipRequest request) {
        User user = userMapper.selectByIdForUpdate(userId);
        if (user == null || user.getDeletedAt() != null || !Objects.equals(user.getStatus(), 1)) {
            throw new AuthException("用户不存在或状态异常");
        }
        String activationKey = request.getActivationKey() == null ? null : request.getActivationKey().trim();
        if (!StringUtils.hasText(activationKey)) {
            throw new AuthException("会员开通幂等键不能为空");
        }
        if (request.getDays() == null || (request.getDays() != 31 && request.getDays() != 93 && request.getDays() != 366)) {
            throw new AuthException("会员天数不合法，仅支持31、93或366天");
        }
        if (request.getLevel() == null || request.getLevel() <= 0) {
            throw new AuthException("会员等级不合法");
        }

        if (activationKey.length() > 128) {
            throw new AuthException("会员开通幂等键不能超过128字符");
        }

        VipActivationRecord record = new VipActivationRecord();
        record.setId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
        record.setActivationKey(activationKey);
        record.setUserId(userId);
        record.setDays(request.getDays());
        record.setLevel(request.getLevel());
        record.setCreatedAt(LocalDateTime.now());
        // Custom INSERT SQL does not receive MyBatis-Plus automatic ID assignment.
        // The unique key arbitrates even requests that target different user rows.
        if (vipActivationRecordMapper.insertIgnore(record) == 0) {
            VipActivationRecord existing = vipActivationRecordMapper.selectByActivationKeyForUpdate(activationKey);
            if (existing == null || !Objects.equals(existing.getUserId(), userId)
                    || !Objects.equals(existing.getDays(), request.getDays())
                    || !Objects.equals(existing.getLevel(), request.getLevel())) {
                throw new AuthException("会员开通幂等键参数冲突");
            }
            return;
        }

        UserProfile profile = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId).last("FOR UPDATE"));
        if (profile == null) {
            initUserProfile(userId);
            profile = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                    .eq(UserProfile::getUserId, userId).last("FOR UPDATE"));
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime base = profile.getVipExpiredAt() != null && profile.getVipExpiredAt().isAfter(now)
                ? profile.getVipExpiredAt() : now;
        LocalDateTime expiredAt = base.plusDays(request.getDays());
        profile.setIsVip(1);
        profile.setVipLevel(Math.max(profile.getVipLevel() == null ? 0 : profile.getVipLevel(), request.getLevel()));
        profile.setVipExpiredAt(expiredAt);
        profile.setVipDaysRemaining(Math.max(0, (int) java.time.Duration.between(now, expiredAt).toDays()));
        int updated = userProfileMapper.update(null, new LambdaUpdateWrapper<UserProfile>()
                .eq(UserProfile::getId, profile.getId())
                .set(UserProfile::getIsVip, profile.getIsVip())
                .set(UserProfile::getVipLevel, profile.getVipLevel())
                .set(UserProfile::getVipExpiredAt, expiredAt)
                .set(UserProfile::getVipDaysRemaining, profile.getVipDaysRemaining()));
        if (updated != 1) {
            throw new AuthException("会员状态更新失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CertificationVO submitCertification(SubmitCertificationRequest request) {
        StpUtil.checkLogin();
        Long userId = StpUtil.getLoginIdAsLong();
        validateCertificationType(request.getCertType());

        Certification certification = new Certification();
        certification.setUserId(userId);
        certification.setCertType(request.getCertType());
        certification.setCertName(request.getCertName().trim());
        certification.setCertProof(request.getCertProof().trim());
        certification.setCertStatus(0);
        certification.setCreatedAt(LocalDateTime.now());
        certificationMapper.insert(certification);
        return toCertificationVO(certification);
    }

    @Override
    public PageResponse<CertificationVO> listMyCertifications(Long pageNum, Long pageSize) {
        StpUtil.checkLogin();
        Long userId = StpUtil.getLoginIdAsLong();
        return listCertificationsByWrapper(new LambdaQueryWrapper<Certification>()
                .eq(Certification::getUserId, userId)
                .orderByDesc(Certification::getCreatedAt), pageNum, pageSize);
    }

    @Override
    public PageResponse<CertificationVO> listCertifications(Integer certType, Integer certStatus, Long pageNum, Long pageSize) {
        requireAdmin();
        return listCertificationsByWrapper(new LambdaQueryWrapper<Certification>()
                .eq(certType != null, Certification::getCertType, certType)
                .eq(certStatus != null, Certification::getCertStatus, certStatus)
                .orderByDesc(Certification::getCreatedAt), pageNum, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CertificationVO reviewCertification(Long certificationId, ReviewCertificationRequest request) {
        requireAdmin();
        Certification certification = certificationMapper.selectById(certificationId);
        if (certification == null) {
            throw new AuthException("认证记录不存在");
        }
        if (!Objects.equals(certification.getCertStatus(), 0)) {
            throw new AuthException("认证记录已审核");
        }
        validateReviewStatus(request.getCertStatus());

        certification.setCertStatus(request.getCertStatus());
        certification.setAuditRemark(request.getAuditRemark().trim());
        certification.setAuditorId(StpUtil.getLoginIdAsLong());
        certification.setAuditTime(LocalDateTime.now());
        certificationMapper.updateById(certification);

        applyCertificationResult(certification);
        return toCertificationVO(certification);
    }

    @Override
    public void logout() {
        StpUtil.checkLogin();
        StpUtil.logout();
    }

    private void validateRegisterRequest(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AuthException("两次输入的密码不一致");
        }
    }

    private void checkRegisterEmailCode(String email, String emailCode) {
        String cachedCode = stringRedisTemplate.opsForValue().get(buildRegisterEmailCodeKey(email));
        if (!StringUtils.hasText(cachedCode)) {
            throw new AuthException("邮箱验证码已过期，请重新发送");
        }
        if (!cachedCode.equals(emailCode)) {
            throw new AuthException("邮箱验证码错误");
        }
    }

    private void clearRegisterEmailCode(String email) {
        stringRedisTemplate.delete(buildRegisterEmailCodeKey(email));
        stringRedisTemplate.delete(buildRegisterEmailLimitKey(email));
    }

    private boolean existsByUsername(String username) {
        return userMapper.selectCount(new LambdaQueryWrapper<User>()
            .eq(User::getUsername, username)
            .isNull(User::getDeletedAt)) > 0;
    }

    private boolean existsByEmail(String email) {
        return userMapper.selectCount(new LambdaQueryWrapper<User>()
            .eq(User::getEmail, email)
            .isNull(User::getDeletedAt)) > 0;
    }

    private String generateNickname() {
        String nickname;
        do {
            nickname = "用户" + generateHashedSuffix(8);
        } while (existsByNickname(nickname));
        return nickname;
    }

    private boolean existsByNickname(String nickname) {
        return userMapper.selectCount(new LambdaQueryWrapper<User>()
            .eq(User::getNickname, nickname)
            .isNull(User::getDeletedAt)) > 0;
    }

    private String generateHashedSuffix(int length) {
        String seed = System.nanoTime() + ":" + request.getRemoteAddr() + ":" + new Random().nextLong();
        String hash = sha256Hex(seed).toUpperCase(Locale.ROOT);
        return hash.substring(0, Math.min(length, hash.length()));
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new AuthException("生成默认昵称失败，请稍后重试");
        }
    }

    private void initUserProfile(Long userId) {
        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setActivityLevel(0L);
        profile.setSchoolVerified(0);
        profile.setCompanyVerified(0);
        profile.setTitleVerified(0);
        profile.setWorkYears(0);
        profile.setViewCount(0);
        profile.setFollowerCount(0);
        profile.setFollowingCount(0);
        profile.setIsVip(0);
        profile.setVipLevel(0);
        profile.setVipDaysRemaining(0);
        userProfileMapper.insert(profile);
    }

    private void validateCertificationType(Integer certType) {
        if (certType == null || (certType != 1 && certType != 2 && certType != 3)) {
            throw new AuthException("认证类型不合法");
        }
    }

    private void validateReviewStatus(Integer certStatus) {
        if (certStatus == null || (certStatus != 1 && certStatus != 2)) {
            throw new AuthException("审核状态不合法");
        }
    }

    private void requireAdmin() {
        StpUtil.checkLogin();
        List<String> roles = roleMapper.selectRoleCodesByUserId(StpUtil.getLoginIdAsLong());
        boolean isAdmin = roles.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .map(String::toUpperCase)
                .anyMatch(role -> "ADMIN".equals(role)
                        || "ROLE_ADMIN".equals(role)
                        || "SUPER_ADMIN".equals(role));
        if (!isAdmin) {
            throw new AuthException("需要管理员权限");
        }
    }

    private PageResponse<CertificationVO> listCertificationsByWrapper(LambdaQueryWrapper<Certification> wrapper,
                                                                       Long pageNum,
                                                                       Long pageSize) {
        long currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long currentSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        Page<Certification> page = certificationMapper.selectPage(new Page<>(currentPage, currentSize), wrapper);
        return PageResponse.of(page.getCurrent(), page.getSize(), page.getTotal(),
                page.getRecords().stream().map(this::toCertificationVO).toList());
    }

    private CertificationVO toCertificationVO(Certification certification) {
        return CertificationVO.builder()
                .id(certification.getId())
                .userId(certification.getUserId())
                .certType(certification.getCertType())
                .certTypeName(resolveCertTypeName(certification.getCertType()))
                .certName(certification.getCertName())
                .certProof(certification.getCertProof())
                .certStatus(certification.getCertStatus())
                .certStatusName(resolveCertStatusName(certification.getCertStatus()))
                .auditRemark(certification.getAuditRemark())
                .auditorId(certification.getAuditorId())
                .auditTime(certification.getAuditTime())
                .createdAt(certification.getCreatedAt())
                .build();
    }

    private String resolveCertTypeName(Integer certType) {
        if (certType == null) {
            return null;
        }
        return switch (certType) {
            case 1 -> "学校认证";
            case 2 -> "企业认证";
            case 3 -> "老师认证";
            default -> "未知";
        };
    }

    private String resolveCertStatusName(Integer certStatus) {
        if (certStatus == null) {
            return null;
        }
        return switch (certStatus) {
            case 0 -> "待审核";
            case 1 -> "已通过";
            case 2 -> "已拒绝";
            default -> "未知";
        };
    }

    private void applyCertificationResult(Certification certification) {
        if (!Objects.equals(certification.getCertStatus(), 1)) {
            return;
        }
        if (Objects.equals(certification.getCertType(), 3)) {
            bindTeacherRole(certification.getUserId());
            return;
        }

        UserProfile profile = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, certification.getUserId())
                .last("limit 1"));
        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(certification.getUserId());
            profile.setActivityLevel(0L);
            profile.setWorkYears(0);
            profile.setViewCount(0);
            profile.setFollowerCount(0);
            profile.setFollowingCount(0);
            profile.setIsVip(0);
            profile.setVipLevel(0);
            profile.setVipDaysRemaining(0);
            userProfileMapper.insert(profile);
        }

        if (Objects.equals(certification.getCertType(), 1)) {
            profile.setSchoolName(certification.getCertName());
            profile.setSchoolVerified(1);
            profile.setSchoolVerifyTime(certification.getAuditTime());
        } else if (Objects.equals(certification.getCertType(), 2)) {
            profile.setCompanyName(certification.getCertName());
            profile.setCompanyVerified(1);
            profile.setCompanyVerifyTime(certification.getAuditTime());
        }
        userProfileMapper.updateById(profile);
    }

    private void bindTeacherRole(Long userId) {
        Role teacherRole = roleMapper.selectById(TEACHER_ROLE_ID);
        if (teacherRole == null) {
            throw new AuthException("教师角色不存在，请先初始化 roles 表数据");
        }
        long count = userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)
                .eq(UserRole::getRoleId, TEACHER_ROLE_ID));
        if (count > 0) {
            return;
        }
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(TEACHER_ROLE_ID);
        userRole.setCreatedAt(LocalDateTime.now());
        userRoleMapper.insert(userRole);
    }

    private UserProfileVO toUserProfileVO(User user) {
        UserProfile profile = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, user.getId())
                .last("limit 1"));
        boolean valid = profile != null && Objects.equals(profile.getIsVip(), 1)
                && profile.getVipExpiredAt() != null && profile.getVipExpiredAt().isAfter(LocalDateTime.now());
        return UserProfileVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername())
                .avatarUrl(resolveAvatarUrl(user.getAvatarUrl()))
                .email(user.getEmail())
                .phone(user.getPhone())
                .gender(user.getGender())
                .birthday(user.getBirthday())
                .signature(user.getSignature())
                .status(user.getStatus())
                .lastLoginTime(user.getLastLoginTime())
                .lastLoginIp(user.getLastLoginIp())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .deletedAt(user.getDeletedAt())
                .schoolName(profile != null && Objects.equals(profile.getSchoolVerified(), 1) ? profile.getSchoolName() : null)
                .companyName(profile != null && Objects.equals(profile.getCompanyVerified(), 1) ? profile.getCompanyName() : null)
                .title(profile != null && Objects.equals(profile.getTitleVerified(), 1) ? profile.getTitle() : null)
                .isVip(valid ? profile.getIsVip() : 0)
                .vipLevel(valid ? profile.getVipLevel() : 0)
                .vipExpiredAt(valid ? profile.getVipExpiredAt() : null)
                .vipDaysRemaining(valid ? profile.getVipDaysRemaining() : 0)
                .build();
    }

    private void bindDefaultRole(Long userId) {
        Role defaultRole = roleMapper.selectDefaultUserRole();
        if (defaultRole == null) {
            throw new AuthException("默认角色 USER 不存在，请先初始化 roles 表数据");
        }
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(defaultRole.getId());
        userRole.setCreatedAt(LocalDateTime.now());
        userRoleMapper.insert(userRole);
    }

    private AuthTokenVO doLogin(User user, Integer loginType, boolean refreshUser, String failReason) {
        return doLogin(user, loginType, refreshUser, failReason, "WEB");
    }

    private AuthTokenVO doLogin(User user, Integer loginType, boolean refreshUser, String failReason, String device) {
        var model = new SaLoginModel().setDevice(device);
        if ("MP_WEIXIN".equals(device)) {
            model.setIsConcurrent(miniLoginConcurrent).setIsShare(false).setTimeout(604800);
        } else if ("WEB".equals(device)) {
            model.setIsConcurrent(false).setIsShare(false);
        }
        StpUtil.login(user.getId(), model);
        String tokenValue = StpUtil.getTokenValue();

        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(getClientIp());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        List<String> roles = roleMapper.selectRoleCodesByUserId(user.getId());
        saveLoginLog(user.getId(), loginType, 1, failReason);

        if (refreshUser) {
            user = userMapper.selectById(user.getId());
        }

        return AuthTokenVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .gender(user.getGender())
                .birthday(user.getBirthday())
                .signature(user.getSignature())
                .roles(roles)
                .tokenName(StpUtil.getTokenName())
                .tokenValue(tokenValue)
                .authorization(buildAuthorization(tokenValue))
                .tokenTimeout(StpUtil.getTokenTimeout())
                .lastLoginTime(user.getLastLoginTime())
                .build();
    }

    private void saveLoginLog(Long userId, Integer loginType, Integer loginStatus, String failReason) {
        String userAgent = defaultString(request.getHeader("User-Agent"));
        String platform = defaultString(request.getHeader("sec-ch-ua-platform"));

        LoginLog loginLog = new LoginLog();
        loginLog.setUserId(userId == null ? 0L : userId);
        loginLog.setLoginType(loginType);
        loginLog.setLoginIp(getClientIp());
        loginLog.setDeviceType(limitLength(resolveDeviceType(userAgent), 50));
        loginLog.setBrowser(limitLength(resolveBrowser(userAgent), 100));
        loginLog.setOs(limitLength(resolveOs(userAgent, platform), 100));
        loginLog.setLoginStatus(loginStatus);
        loginLog.setFailReason(limitLength(failReason, 200));
        loginLog.setCreatedAt(LocalDateTime.now());
        loginLogMapper.insert(loginLog);
    }

    private void sendEmail(String to, String code) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(to);
            helper.setSubject("Matu 注册验证码");
            helper.setText(buildEmailContent(code), true);
            javaMailSender.send(message);
        } catch (Exception e) {
            log.error("发送注册验证码邮件失败, to={}, from={}", to, mailFrom, e);
            throw new AuthException("验证码发送失败，请稍后重试");
        }
    }

    private String buildEmailContent(String code) {
        return "<div style=\"font-family:Arial,sans-serif;line-height:1.8;color:#333;\">"
            + "<h2>Matu 邮箱验证码</h2>"
            + "<p>您好，您本次注册的验证码为：</p>"
            + "<div style=\"font-size:28px;font-weight:bold;color:#1677ff;letter-spacing:6px;\">"
            + code
            + "</div>"
            + "<p>验证码 "
            + registerEmailCodeExpireMinutes
            + " 分钟内有效，请勿泄露给他人。</p>"
            + "</div>";
    }

    private String generateEmailCode() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }

    private String buildRegisterEmailCodeKey(String email) {
        return REGISTER_EMAIL_CODE_KEY_PREFIX + email.toLowerCase(Locale.ROOT);
    }

    private String buildRegisterEmailLimitKey(String email) {
        return REGISTER_EMAIL_LIMIT_KEY_PREFIX + email.toLowerCase(Locale.ROOT);
    }

    private String resolveDeviceType(String userAgent) {
        String ua = userAgent.toLowerCase(Locale.ROOT);
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) {
            return "MOBILE";
        }
        if (ua.contains("ipad") || ua.contains("tablet")) {
            return "TABLET";
        }
        return "PC";
    }

    private String resolveBrowser(String userAgent) {
        String ua = userAgent.toLowerCase(Locale.ROOT);
        if (ua.contains("edg/")) {
            return "Edge";
        }
        if (ua.contains("chrome/") && !ua.contains("edg/")) {
            return "Chrome";
        }
        if (ua.contains("firefox/")) {
            return "Firefox";
        }
        if (ua.contains("safari/") && !ua.contains("chrome/")) {
            return "Safari";
        }
        if (ua.contains("opera") || ua.contains("opr/")) {
            return "Opera";
        }
        if (ua.contains("msie") || ua.contains("trident/")) {
            return "IE";
        }
        return StringUtils.hasText(userAgent) ? userAgent : "Unknown";
    }

    private String resolveOs(String userAgent, String platform) {
        if (StringUtils.hasText(platform)) {
            return trimPlatform(platform);
        }

        String ua = userAgent.toLowerCase(Locale.ROOT);
        if (ua.contains("windows")) {
            return "Windows";
        }
        if (ua.contains("mac os") || ua.contains("macintosh")) {
            return "macOS";
        }
        if (ua.contains("android")) {
            return "Android";
        }
        if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ios")) {
            return "iOS";
        }
        if (ua.contains("linux")) {
            return "Linux";
        }
        return StringUtils.hasText(userAgent) ? userAgent : "Unknown";
    }

    private String trimPlatform(String platform) {
        String normalized = platform.replace('"', ' ').trim();
        return normalized.length() > 100 ? normalized.substring(0, 100) : normalized;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private String limitLength(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    private String getClientIp() {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String buildAuthorization(String tokenValue) {
        return "Bearer " + tokenValue;
    }

    private PageResponse<UserFollowVO> listFollowingByUserId(Long targetUserId, Long currentUserId, Long pageNum, Long pageSize) {
        long current = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);

        Page<UserFollow> page = userFollowMapper.selectPage(
                new Page<>(current, size),
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowerId, targetUserId)
                        .orderByDesc(UserFollow::getCreatedAt)
        );

        List<UserFollowVO> records = page.getRecords().stream()
                .map(UserFollow::getFollowingId)
                .map(userMapper::selectById)
                .filter(Objects::nonNull)
                .map(user -> toUserFollowVO(user, currentUserId))
                .toList();

        return PageResponse.of(current, size, page.getTotal(), records);
    }

    private PageResponse<UserFollowVO> listFollowersByUserId(Long targetUserId, Long currentUserId, Long pageNum, Long pageSize) {
        long current = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);

        Page<UserFollow> page = userFollowMapper.selectPage(
                new Page<>(current, size),
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowingId, targetUserId)
                        .orderByDesc(UserFollow::getCreatedAt)
        );

        List<UserFollowVO> records = page.getRecords().stream()
                .map(UserFollow::getFollowerId)
                .map(userMapper::selectById)
                .filter(Objects::nonNull)
                .map(user -> toUserFollowVO(user, currentUserId))
                .toList();

        return PageResponse.of(current, size, page.getTotal(), records);
    }

    private UserFollowVO toUserFollowVO(User user, Long currentUserId) {
        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>()
                        .eq(UserProfile::getUserId, user.getId())
                        .last("limit 1")
        );

        int isFollowed = 0;
        if (currentUserId != null && !Objects.equals(currentUserId, user.getId())) {
            Long count = userFollowMapper.selectCount(
                    new LambdaQueryWrapper<UserFollow>()
                            .eq(UserFollow::getFollowerId, currentUserId)
                            .eq(UserFollow::getFollowingId, user.getId())
            );
            isFollowed = count != null && count > 0 ? 1 : 0;
        }

        return UserFollowVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatarUrl(resolveAvatarUrl(user.getAvatarUrl()))
                .signature(user.getSignature())
                .isFollowed(isFollowed)
                .followerCount(profile == null || profile.getFollowerCount() == null ? 0 : profile.getFollowerCount())
                .followingCount(profile == null || profile.getFollowingCount() == null ? 0 : profile.getFollowingCount())
                .build();
    }

    private User getUserOrThrow(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new AuthException("用户不存在");
        }
        return user;
    }

    private void increaseFollowerCount(Long userId) {
        UserProfile profile = getOrInitUserProfile(userId);
        profile.setFollowerCount((profile.getFollowerCount() == null ? 0 : profile.getFollowerCount()) + 1);
        userProfileMapper.updateById(profile);
    }

    private void decreaseFollowerCount(Long userId) {
        UserProfile profile = getOrInitUserProfile(userId);
        int current = profile.getFollowerCount() == null ? 0 : profile.getFollowerCount();
        profile.setFollowerCount(Math.max(0, current - 1));
        userProfileMapper.updateById(profile);
    }

    private void increaseFollowingCount(Long userId) {
        UserProfile profile = getOrInitUserProfile(userId);
        profile.setFollowingCount((profile.getFollowingCount() == null ? 0 : profile.getFollowingCount()) + 1);
        userProfileMapper.updateById(profile);
    }

    private void decreaseFollowingCount(Long userId) {
        UserProfile profile = getOrInitUserProfile(userId);
        int current = profile.getFollowingCount() == null ? 0 : profile.getFollowingCount();
        profile.setFollowingCount(Math.max(0, current - 1));
        userProfileMapper.updateById(profile);
    }

    private UserProfile getOrInitUserProfile(Long userId) {
        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>()
                        .eq(UserProfile::getUserId, userId)
                        .last("limit 1")
        );
        if (profile != null) {
            return profile;
        }
        initUserProfile(userId);
        return userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>()
                        .eq(UserProfile::getUserId, userId)
                        .last("limit 1")
        );
    }

    private String resolveAvatarUrl(String avatarUrl) {
        return StringUtils.hasText(avatarUrl)
                ? avatarUrl
                : DEFAULT_AVATAR_URL;
    }
}
