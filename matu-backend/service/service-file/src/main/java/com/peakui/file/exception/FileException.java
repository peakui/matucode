package com.peakui.file.exception;

import com.peakui.common.exception.BusinessException;

/**
 * 文件服务业务异常。
 */
public class FileException extends BusinessException {

    public FileException(String message) {
        super(message);
    }
}
