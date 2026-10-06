package com.peakui.oj.judge;

public enum JudgeStatusEnum {
    WAITING(0, "等待判题"),
    ACCEPTED(1, "答案正确"),
    WRONG_ANSWER(2, "答案错误"),
    RUNTIME_ERROR(3, "运行错误"),
    TIME_LIMIT_EXCEEDED(4, "超出时间限制"),
    MEMORY_LIMIT_EXCEEDED(5, "超出内存限制"),
    COMPILE_ERROR(6, "编译错误"),
    SYSTEM_ERROR(7, "系统错误");

    private final int code;
    private final String message;

    JudgeStatusEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
