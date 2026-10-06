package com.peakui.pay.constant;

public final class OrderStatus {
    public static final int PENDING = 0;
    public static final int PAID = 1;
    public static final int CLOSED = 2;
    public static final int REFUNDED = 3;
    public static final int PARTIAL_REFUNDED = 4;

    private OrderStatus() {
    }
}
