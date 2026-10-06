package com.peakui.check.exception;

import com.peakui.common.exception.BusinessException;

/**
 * 打卡业务异常。
 */
public class CheckException extends BusinessException {

    public CheckException(String message) {
        super(message);
    }
}
