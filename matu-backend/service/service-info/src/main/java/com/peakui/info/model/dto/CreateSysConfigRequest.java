package com.peakui.info.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateSysConfigRequest {

    @NotBlank(message = "配置键不能为空")
    @Size(max = 128, message = "配置键长度不能超过128个字符")
    private String configKey;

    private String configValue;

    @Size(max = 255, message = "配置说明长度不能超过255个字符")
    private String description;

    @Size(max = 64, message = "配置分组长度不能超过64个字符")
    private String groupName;

    @Min(value = 0, message = "公开状态不合法")
    @Max(value = 1, message = "公开状态不合法")
    private Integer isPublic;
}
