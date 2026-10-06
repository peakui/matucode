package com.peakui.pay.service;

import com.peakui.pay.model.dto.RefundRequest;
import com.peakui.pay.model.vo.RefundVO;

public interface RefundService {
    RefundVO refund(RefundRequest request);

    /** Admin-triggered refund: skips the caller-ownership check. */
    RefundVO refundAsAdmin(RefundRequest request);
}
