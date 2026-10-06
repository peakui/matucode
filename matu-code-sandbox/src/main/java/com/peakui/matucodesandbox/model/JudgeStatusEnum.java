package com.peakui.matucodesandbox.model;

public enum JudgeStatusEnum {

    WAITING(0, "等待判题"),
    ACCEPTED(1, "答案正确"),
    WRONG_ANSWER(2, "答案错误"),
    RUNTIME_ERROR(3, "运行错误"),
    TIME_LIMIT_EXCEEDED(4, "超出时间限制"),
    MEMORY_LIMIT_EXCEEDED(5, "超出内存限制"),
    COMPILE_ERROR(6, "编译错误"),
    SYSTEM_ERROR(7, "系统错误");

    private final Integer value;

    private final String text;

    JudgeStatusEnum(Integer value, String text) {
        this.value = value;
        this.text = text;
    }

    public Integer getValue() {
        return value;
    }

    public String getText() {
        return text;
    }
}
