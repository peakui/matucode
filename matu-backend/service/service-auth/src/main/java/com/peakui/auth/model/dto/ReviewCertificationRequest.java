package com.peakui.auth.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewCertificationRequest {

    @NotNull(message = "审核结果不能为空")
    private Integer certStatus;

    @NotBlank(message = "审核备注不能为空")
    private String auditRemark;
}
