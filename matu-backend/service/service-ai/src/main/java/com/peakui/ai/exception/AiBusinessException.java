package com.peakui.ai.exception;

public class AiBusinessException extends RuntimeException {
    private final int status;

    public AiBusinessException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
