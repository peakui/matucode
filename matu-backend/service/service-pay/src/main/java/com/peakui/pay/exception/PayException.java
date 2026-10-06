package com.peakui.pay.exception;

import com.peakui.common.exception.BusinessException;

public class PayException extends BusinessException {
    public PayException(String message) {
        super(message);
    }
}
