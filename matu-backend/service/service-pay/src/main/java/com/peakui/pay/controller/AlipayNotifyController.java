package com.peakui.pay.controller;

import com.peakui.pay.service.AlipayNotifyService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/pay/alipay")
public class AlipayNotifyController {

    private final AlipayNotifyService alipayNotifyService;

    @PostMapping(value = "/notify", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String notify(@RequestParam Map<String, String> params) {
        return alipayNotifyService.handleNotify(params) ? "success" : "failure";
    }
}
