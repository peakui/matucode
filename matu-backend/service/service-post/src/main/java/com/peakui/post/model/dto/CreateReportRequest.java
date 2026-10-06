package com.peakui.post.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 举报请求。
 */
@Data
public class CreateReportRequest {

    private Long commentId;

    @NotNull(message = "举报类型不能为空")
    private Integer reportType;

    @NotBlank(message = "举报原因不能为空")
    @Size(max = 500, message = "举报原因长度不能超过500个字符")
    private String reportReason;
}
