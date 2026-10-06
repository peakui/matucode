package com.peakui.info.handler;

import cn.dev33.satoken.exception.NotRoleException;
import com.peakui.common.exception.BaseGlobalExceptionHandler;
import com.peakui.common.result.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler extends BaseGlobalExceptionHandler {

    @ExceptionHandler(NotRoleException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleNotRoleException(NotRoleException e) {
        return ApiResponse.fail(403, "无权限访问信息服务资源");
    }
}
