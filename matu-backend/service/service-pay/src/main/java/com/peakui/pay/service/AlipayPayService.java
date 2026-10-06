package com.peakui.pay.service;

import com.peakui.pay.model.dto.CreateAlipayPageRequest;
import com.peakui.pay.model.dto.CreateAlipayQrRequest;
import com.peakui.pay.model.vo.AlipayPagePayVO;
import com.peakui.pay.model.vo.AlipayQrPayVO;
import com.peakui.pay.model.vo.TransactionVO;

public interface AlipayPayService {
    AlipayQrPayVO createQrPay(CreateAlipayQrRequest request);

    AlipayPagePayVO createPagePay(CreateAlipayPageRequest request);

    TransactionVO getCurrentUserTransaction(String transactionNo);
}
