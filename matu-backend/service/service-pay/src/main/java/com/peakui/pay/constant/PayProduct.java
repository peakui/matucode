package com.peakui.pay.constant;

import com.peakui.pay.exception.PayException;
import java.math.BigDecimal;

public enum PayProduct {
    VIP_MONTH("VIP_MONTH", "VIP月卡", new BigDecimal("19.90")),
    VIP_QUARTER("VIP_QUARTER", "VIP季卡", new BigDecimal("49.90")),
    VIP_YEAR("VIP_YEAR", "VIP年卡", new BigDecimal("159.90"));

    private final String code;
    private final String productName;
    private final BigDecimal amount;

    PayProduct(String code, String productName, BigDecimal amount) {
        this.code = code;
        this.productName = productName;
        this.amount = amount;
    }

    public String code() {
        return code;
    }

    public String productName() {
        return productName;
    }

    public BigDecimal amount() {
        return amount;
    }

    public static PayProduct of(String code) {
        for (PayProduct product : values()) {
            if (product.code.equals(code)) {
                return product;
            }
        }
        throw new PayException("商品不存在");
    }
}
