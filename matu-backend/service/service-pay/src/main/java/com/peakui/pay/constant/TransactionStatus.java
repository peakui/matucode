package com.peakui.pay.constant;

public final class TransactionStatus {
    public static final int PENDING = 0;
    public static final int SUCCESS = 1;
    public static final int FAILED = 2;
    public static final int CLOSED = 3;
    public static final int PROCESSING = 4;

    private TransactionStatus() {
    }
}
