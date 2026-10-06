package com.peakui.qa.exception;

import com.peakui.common.exception.BusinessException;

/**
 * 问答业务异常。
 */
public class QaException extends BusinessException {

    public QaException(String message) {
        super(message);
    }
}
