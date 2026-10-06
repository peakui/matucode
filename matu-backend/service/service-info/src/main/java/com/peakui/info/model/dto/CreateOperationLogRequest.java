package com.peakui.info.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateOperationLogRequest {

    private Long userId;

    @Size(max = 64, message = "用户名长度不能超过64个字符")
    private String username;

    @NotBlank(message = "功能模块不能为空")
    @Size(max = 64, message = "功能模块长度不能超过64个字符")
    private String module;

    @NotBlank(message = "操作类型不能为空")
    @Size(max = 64, message = "操作类型长度不能超过64个字符")
    private String action;

    @Size(max = 64, message = "目标资源类型长度不能超过64个字符")
    private String targetType;

    private Long targetId;
    private String detail;

    @NotBlank(message = "操作IP不能为空")
    @Size(max = 45, message = "操作IP长度不能超过45个字符")
    private String ipAddress;

    @Size(max = 512, message = "浏览器UA长度不能超过512个字符")
    private String userAgent;

    @Min(value = 0, message = "操作结果不合法")
    @Max(value = 1, message = "操作结果不合法")
    private Integer result;

    @Size(max = 512, message = "错误信息长度不能超过512个字符")
    private String errorMsg;
}
