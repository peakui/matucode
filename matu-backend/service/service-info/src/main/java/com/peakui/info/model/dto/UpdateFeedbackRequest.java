package com.peakui.info.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateFeedbackRequest {

    @Min(value = 0, message = "反馈状态不合法")
    @Max(value = 4, message = "反馈状态不合法")
    private Integer status;

    @Min(value = 0, message = "反馈优先级不合法")
    @Max(value = 3, message = "反馈优先级不合法")
    private Integer priority;

    private Long assigneeId;

    @Size(max = 20000, message = "回复内容长度不能超过20000个字符")
    private String replyContent;
}
