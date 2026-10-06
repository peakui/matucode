package com.peakui.post.exception;

import com.peakui.common.exception.BusinessException;

/**
 * 帖子业务异常。
 */
public class PostException extends BusinessException {

    public PostException(String message) {
        super(message);
    }
}
