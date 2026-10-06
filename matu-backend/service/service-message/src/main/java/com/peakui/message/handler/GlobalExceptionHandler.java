package com.peakui.message.handler;

import com.peakui.common.exception.BaseGlobalExceptionHandler;
import com.peakui.common.exception.CommonError;
import com.peakui.common.result.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends BaseGlobalExceptionHandler {

    @Override
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("service-message unexpected exception", e);
        return ApiResponse.fail(CommonError.INTERNAL_SERVER_ERROR.code(), CommonError.INTERNAL_SERVER_ERROR.message());
    }
}
