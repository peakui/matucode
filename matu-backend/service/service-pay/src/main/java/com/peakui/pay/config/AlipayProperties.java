package com.peakui.pay.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "pay.alipay")
public class AlipayProperties {
    @NotBlank
    private String appId;
    @NotBlank
    private String merchantPrivateKey;
    @NotBlank
    private String alipayPublicKey;
    @NotBlank
    private String gatewayUrl;
    @NotBlank
    private String notifyUrl;
    private String returnUrl;
    @NotBlank
    private String signType = "RSA2";
    @NotBlank
    private String charset = "UTF-8";
    @NotBlank
    private String format = "json";
}
