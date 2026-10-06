package com.peakui.message.collab.ot;

import com.peakui.message.exception.MessageException;

public enum OperationType {
    INSERT(1, "insert"),
    DELETE(2, "delete"),
    NOOP(0, "noop");

    private final int code;
    private final String value;

    OperationType(int code, String value) {
        this.code = code;
        this.value = value;
    }

    public int code() {
        return code;
    }

    public String value() {
        return value;
    }

    public static OperationType fromValue(String value) {
        for (OperationType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new MessageException("不支持的编辑操作");
    }

    public static OperationType fromCode(Integer code) {
        for (OperationType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new MessageException("不支持的编辑操作");
    }
}
