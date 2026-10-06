package com.peakui.auth.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.peakui.common.dashboard.DashboardAuthStatsVO;
import com.peakui.common.dashboard.DashboardCertificationRankItemDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.peakui.auth.mapper.CertificationMapper;
import com.peakui.auth.model.dto.ActivateVipRequest;
import com.peakui.auth.model.dto.EmailCodeRequest;
import com.peakui.auth.model.dto.LoginRequest;
import com.peakui.auth.model.dto.RegisterRequest;
import com.peakui.auth.model.dto.ReviewCertificationRequest;
import com.peakui.auth.model.dto.SubmitCertificationRequest;
import com.peakui.auth.model.dto.UpdateAvatarRequest;
import com.peakui.auth.model.dto.UpdateCurrentUserRequest;
import com.peakui.auth.model.vo.ActiveDeveloperVO;
import com.peakui.auth.model.vo.AuthTokenVO;
import com.peakui.common.exception.CommonError;
import com.peakui.auth.model.vo.CertificationVO;
import com.peakui.auth.model.vo.FollowStatusVO;
import com.peakui.auth.model.vo.UserBriefVO;
import com.peakui.auth.model.vo.UserFollowVO;
import com.peakui.auth.model.vo.UserProfileVO;
import com.peakui.auth.model.vo.UserRolesVO;
import com.peakui.auth.model.vo.VipStatusVO;
import com.peakui.auth.service.AuthService;
import com.peakui.common.result.ApiResponse;
import com.peakui.common.result.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;


@Tag(name = "认证接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final CertificationMapper certificationMapper;

    @Value("${security.vip.service-token:}")
    private String vipServiceToken;

    @GetMapping("/internal/dashboard/stats")
    public ApiResponse<DashboardAuthStatsVO> dashboardStats() {
        List<DashboardCertificationRankItemDTO> rankings = certificationMapper.selectCertificationRankings()
                .stream()
                .map(this::toCertificationRankItem)
                .toList();
        return ApiResponse.success(DashboardAuthStatsVO.builder()
                .certificationCount(certificationMapper.selectCount(new LambdaQueryWrapper<com.peakui.auth.model.entity.Certification>().eq(com.peakui.auth.model.entity.Certification::getCertStatus, 1)))
                .certificationRankings(rankings)
                .build());
    }

    private DashboardCertificationRankItemDTO toCertificationRankItem(Map<String, Object> item) {
        Object count = item.get("count");
        return DashboardCertificationRankItemDTO.builder()
                .name(String.valueOf(item.get("name")))
                .type(String.valueOf(item.get("type")))
                .count(count instanceof Number number ? number.longValue() : 0L)
                .build();
    }

    @Operation(summary = "发送注册邮箱验证码")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "发送成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "邮箱已存在或发送过于频繁")
    })
    @PostMapping("/register/email-code")
    public ApiResponse<Void> sendRegisterEmailCode(@Valid @RequestBody EmailCodeRequest request) {
        authService.sendRegisterEmailCode(request.getEmail());
        return ApiResponse.success("验证码已发送，请注意查收", null);
    }

    @Operation(
            summary = "注册",
            description = "用户填写用户名、邮箱、邮箱验证码和密码完成注册。昵称可选填；若不填写，系统会基于哈希生成昵称。注册成功后自动登录。",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "注册请求体",
                    content = @Content(
                            examples = @ExampleObject(value = "{\n  \"username\": \"testuser\",\n  \"email\": \"demo@qq.com\",\n  \"emailCode\": \"123456\",\n  \"phone\": \"13800138000\",\n  \"password\": \"123456\",\n  \"confirmPassword\": \"123456\"\n}")
                    )
            )
    )
    @PostMapping("/register")
    public ApiResponse<AuthTokenVO> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success("注册成功", authService.register(request));
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public ApiResponse<AuthTokenVO> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("登录成功", authService.login(request));
    }

    @Operation(summary = "获取当前登录用户")
    @GetMapping("/me")
    public ApiResponse<AuthTokenVO> currentUser() {
        return ApiResponse.success(authService.currentUser());
    }

    @Operation(summary = "修改当前用户基础信息")
    @PutMapping("/me")
    public ApiResponse<AuthTokenVO> updateCurrentUser(@Valid @RequestBody UpdateCurrentUserRequest request) {
        return ApiResponse.success(CommonError.UPDATE_SUCCESS.message(), authService.updateCurrentUser(request));
    }

    @Operation(summary = "修改当前用户头像")
    @PutMapping("/me/avatar")
    public ApiResponse<AuthTokenVO> updateAvatar(@Valid @RequestBody UpdateAvatarRequest request) {
        return ApiResponse.success(CommonError.UPDATE_SUCCESS.message(), authService.updateAvatar(request));
    }

    @Operation(summary = "关注用户")
    @PostMapping("/users/{userId}/follow")
    public ApiResponse<Void> followUser(@PathVariable Long userId) {
        authService.followUser(userId);
        return ApiResponse.success(CommonError.FOLLOW_SUCCESS.message(), null);
    }

    @Operation(summary = "取消关注用户")
    @DeleteMapping("/users/{userId}/follow")
    public ApiResponse<Void> unfollowUser(@PathVariable Long userId) {
        authService.unfollowUser(userId);
        return ApiResponse.success(CommonError.UNFOLLOW_SUCCESS.message(), null);
    }

    @Operation(summary = "我的关注列表")
    @GetMapping("/me/following")
    public ApiResponse<PageResponse<UserFollowVO>> listMyFollowing(@RequestParam(defaultValue = "1") Long pageNum,
                                                                   @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.success(authService.listMyFollowing(pageNum, pageSize));
    }

    @Operation(summary = "我的粉丝列表")
    @GetMapping("/me/followers")
    public ApiResponse<PageResponse<UserFollowVO>> listMyFollowers(@RequestParam(defaultValue = "1") Long pageNum,
                                                                   @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.success(authService.listMyFollowers(pageNum, pageSize));
    }

    @Operation(summary = "指定用户关注列表")
    @GetMapping("/users/{userId}/following")
    public ApiResponse<PageResponse<UserFollowVO>> listUserFollowing(@PathVariable Long userId,
                                                                     @RequestParam(defaultValue = "1") Long pageNum,
                                                                     @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.success(authService.listUserFollowing(userId, pageNum, pageSize));
    }

    @Operation(summary = "指定用户粉丝列表")
    @GetMapping("/users/{userId}/followers")
    public ApiResponse<PageResponse<UserFollowVO>> listUserFollowers(@PathVariable Long userId,
                                                                     @RequestParam(defaultValue = "1") Long pageNum,
                                                                     @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.success(authService.listUserFollowers(userId, pageNum, pageSize));
    }

    @Operation(summary = "查询是否已关注用户")
    @GetMapping("/users/{userId}/follow-status")
    public ApiResponse<FollowStatusVO> getFollowStatus(@PathVariable Long userId) {
        return ApiResponse.success(authService.getFollowStatus(userId));
    }

    @Operation(summary = "获取活动开发者 Top10")
    @GetMapping("/users/active-developers")
    public ApiResponse<List<ActiveDeveloperVO>> listActiveDevelopers() {
        return ApiResponse.success(authService.listActiveDevelopers());
    }

    @Operation(summary = "提交认证申请")
    @PostMapping("/certifications")
    public ApiResponse<CertificationVO> submitCertification(@Valid @RequestBody SubmitCertificationRequest request) {
        return ApiResponse.success(CommonError.SUBMIT_SUCCESS.message(), authService.submitCertification(request));
    }

    @Operation(summary = "我的认证申请列表")
    @GetMapping("/me/certifications")
    public ApiResponse<PageResponse<CertificationVO>> listMyCertifications(@RequestParam(defaultValue = "1") Long pageNum,
                                                                            @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.success(authService.listMyCertifications(pageNum, pageSize));
    }

    @Operation(summary = "管理员认证申请列表")
    @GetMapping("/internal/certifications")
    public ApiResponse<PageResponse<CertificationVO>> listCertifications(@RequestParam(required = false) Integer certType,
                                                                         @RequestParam(required = false) Integer certStatus,
                                                                         @RequestParam(defaultValue = "1") Long pageNum,
                                                                         @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.success(authService.listCertifications(certType, certStatus, pageNum, pageSize));
    }

    @Operation(summary = "管理员审核认证申请")
    @PostMapping("/internal/certifications/{certificationId}/review")
    public ApiResponse<CertificationVO> reviewCertification(@PathVariable Long certificationId,
                                                            @Valid @RequestBody ReviewCertificationRequest request) {
        return ApiResponse.success(CommonError.REVIEW_SUCCESS.message(), authService.reviewCertification(certificationId, request));
    }

    @GetMapping({"/internal/users/{userId}", "/users/{userId}"})
    public ApiResponse<UserProfileVO> getUserProfile(@PathVariable Long userId) {
        return ApiResponse.success(authService.getUserProfile(userId));
    }

    @GetMapping("/internal/users/batch")
    public ApiResponse<List<UserBriefVO>> getUserBriefs(@RequestParam("ids") List<Long> ids) {
        return ApiResponse.success(authService.getUserBriefs(ids));
    }

    @GetMapping({"/internal/users/by-username/{username}", "/users/by-username/{username}"})
    public ApiResponse<UserProfileVO> getUserProfileByUsername(@PathVariable String username) {
        return ApiResponse.success(authService.getUserProfileByUsername(username));
    }

    @GetMapping("/internal/users/{userId}/roles")
    public ApiResponse<UserRolesVO> getUserRoles(@PathVariable Long userId) {
        return ApiResponse.success(authService.getUserRoles(userId));
    }

    @GetMapping("/me/vip")
    public ApiResponse<VipStatusVO> currentVip() {
        StpUtil.checkLogin();
        return ApiResponse.success(authService.getVipStatus(StpUtil.getLoginIdAsLong()));
    }

    @GetMapping("/internal/users/{userId}/vip")
    public ApiResponse<VipStatusVO> getVipStatus(@PathVariable Long userId,
                                                  HttpServletRequest request) {
        verifyVipServiceToken(request);
        return ApiResponse.success(authService.getVipStatus(userId));
    }

    @PostMapping("/internal/users/{userId}/vip/activate")
    public ApiResponse<Void> activateVip(@PathVariable Long userId,
                                         @Valid @RequestBody ActivateVipRequest request,
                                         HttpServletRequest httpRequest) {
        verifyVipServiceToken(httpRequest);
        authService.activateVip(userId, request);
        return ApiResponse.success("会员开通成功", null);
    }

    private void verifyVipServiceToken(HttpServletRequest request) {
        String provided = request.getHeader("X-Vip-Service-Token");
        if (!org.springframework.util.StringUtils.hasText(vipServiceToken)
                || !org.springframework.util.StringUtils.hasText(provided)
                || !MessageDigest.isEqual(vipServiceToken.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无效的服务凭证");
        }
    }

    @PostMapping("/internal/users/{userId}/activity-level/increase")
    public ApiResponse<Void> increaseActivityLevel(@PathVariable Long userId) {
        authService.increaseActivityLevel(userId);
        return ApiResponse.success("活跃度更新成功", null);
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.success(CommonError.LOGOUT_SUCCESS.message(), null);
    }
}
