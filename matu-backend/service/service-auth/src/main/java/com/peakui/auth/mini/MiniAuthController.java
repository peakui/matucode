package com.peakui.auth.mini;

import cn.dev33.satoken.stp.StpUtil;
import com.peakui.auth.model.dto.LoginRequest;
import com.peakui.common.result.ApiResponse;
import com.peakui.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/auth/mini")
@RequiredArgsConstructor
public class MiniAuthController {
    private final MiniAuthService service;
    private final MiniLoginGuard guard;
    private final WechatClient wechat;
    private final HttpServletRequest request;

    private void limit(String account) {
        // Intentionally do not trust arbitrary X-Forwarded-For headers. Configure trusted proxies at deployment.
        guard.check(request.getRemoteAddr(), account);
    }
    @GetMapping("/capabilities")
    public ApiResponse<?> capabilities() { return ApiResponse.success(Map.of("wechatLogin", wechat.enabled())); }
    @PostMapping("/login")
    public ApiResponse<?> login(@Valid @RequestBody LoginRequest data) {
        limit(data.getAccount()); return ApiResponse.success(service.passwordLogin(data.getAccount().trim(), data.getPassword()));
    }
    @PostMapping("/wechat/login")
    public ApiResponse<?> wechat(@Valid @RequestBody MiniModels.CodeRequest data) {
        limit(null); return ApiResponse.success(service.wechatLogin(data.code()));
    }
    @PostMapping("/wechat/complete")
    public ApiResponse<?> complete(@Valid @RequestBody MiniModels.CompleteRequest data) {
        limit(data.account()); return ApiResponse.success(service.complete(data));
    }
    @GetMapping("/me")
    public ApiResponse<?> me() { return ApiResponse.success(service.me()); }
    @PostMapping("/logout")
    public ApiResponse<?> logout() { StpUtil.checkLogin(); StpUtil.logout(); return ApiResponse.success(null); }
    @PostMapping("/wechat/unlink")
    public ApiResponse<?> unlink(@Valid @RequestBody MiniModels.PasswordRequest data) {
        StpUtil.checkLogin(); limit(StpUtil.getLoginIdAsString()); service.unlink(data.password()); return ApiResponse.success(null);
    }
    @PostMapping("/wechat/bind")
    public ApiResponse<?> bind(@Valid @RequestBody MiniModels.BindCodeRequest data) {
        StpUtil.checkLogin(); limit(StpUtil.getLoginIdAsString()); return ApiResponse.success(service.bindCurrent(data));
    }
    @PostMapping("/deletion-request")
    public ApiResponse<?> deletion(@Valid @RequestBody MiniModels.DeleteRequest data) {
        StpUtil.checkLogin(); limit(StpUtil.getLoginIdAsString()); service.requestDeletion(data);
        return ApiResponse.success(Map.of("status", "PENDING_REVIEW"));
    }
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> error(BusinessException error) {
        int status = error.getCode() != null && error.getCode() >= 400 && error.getCode() < 600 ? error.getCode() : 400;
        return ResponseEntity.status(status).body(ApiResponse.fail(status, error.getMessage()));
    }
}
