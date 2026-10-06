package com.peakui.ai.handler;

import com.peakui.ai.exception.AiBusinessException;
import com.peakui.common.result.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AiExceptionHandler {
    @ExceptionHandler(AiBusinessException.class)
    public ApiResponse<Void> handleAi(AiBusinessException exception) {
        return ApiResponse.fail(exception.getStatus(), exception.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ApiResponse<Void> handleValidation(Exception exception) {
        return ApiResponse.fail(400, "请求参数不合法");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleOther(Exception exception) {
        return ApiResponse.fail(500, "AI 服务暂时不可用");
    }
}
