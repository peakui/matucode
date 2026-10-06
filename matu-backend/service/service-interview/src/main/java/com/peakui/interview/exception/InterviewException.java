package com.peakui.interview.exception;

import com.peakui.common.exception.BusinessException;

public class InterviewException extends BusinessException {
    private final long retryAfter;
    public InterviewException(String message) { this(400, message); }
    public InterviewException(Integer code, String message) { this(code, message, 60); }
    public InterviewException(Integer code, String message, long retryAfter) {
        super(code, message);
        this.retryAfter = retryAfter;
    }
    public long getRetryAfter() { return retryAfter; }
}
