package com.peakui.auth.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ActivateVipRequest {
    @NotBlank(message = "会员开通幂等键不能为空")
    @Size(max = 128, message = "会员开通幂等键不能超过128字符")
    private String activationKey;

    @NotNull(message = "会员天数不能为空")
    @Min(value = 1, message = "会员天数必须大于0")
    @Max(value = 366, message = "会员天数不能超过366")
    private Integer days;

    @NotNull(message = "会员等级不能为空")
    @Min(value = 1, message = "会员等级必须大于0")
    private Integer level;
}
