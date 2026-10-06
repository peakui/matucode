package com.peakui.pay.service;

import java.util.Map;

public interface AlipayNotifyService {
    boolean handleNotify(Map<String, String> params);
}
