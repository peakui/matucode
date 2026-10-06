package com.peakui.auth.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitCertificationRequest {

    @NotNull(message = "认证类型不能为空")
    private Integer certType;

    @NotBlank(message = "认证名称不能为空")
    private String certName;

    @NotBlank(message = "证明材料不能为空")
    private String certProof;
}
