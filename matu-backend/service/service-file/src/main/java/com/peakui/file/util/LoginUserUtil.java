package com.peakui.file.util;

import com.peakui.file.exception.FileException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 登录用户工具。
 */
@Component
@RequiredArgsConstructor
public class LoginUserUtil {

    private final HttpServletRequest request;

    public Long getCurrentUserId() {
        String userId = request.getHeader("X-User-Id");
        if (!StringUtils.hasText(userId)) {
            throw new FileException("未登录或用户信息缺失");
        }
        try {
            return Long.parseLong(userId.trim());
        } catch (NumberFormatException e) {
            throw new FileException("当前登录用户无效");
        }
    }
}
