package com.peakui.auth.exception;

import com.peakui.common.exception.BusinessException;

/**
 * 认证业务异常。
 */
public class AuthException extends BusinessException {

    public AuthException(String message) {
        super(message);
    }

    public AuthException(Integer code, String message) {
        super(code, message);
    }
}
