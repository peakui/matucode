package com.peakui.info.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CreateFeedbackRequest {

    @Size(max = 255, message = "联系邮箱长度不能超过255个字符")
    private String contactEmail;

    @Min(value = 0, message = "反馈类型不合法")
    @Max(value = 4, message = "反馈类型不合法")
    private Integer type;

    @NotBlank(message = "反馈标题不能为空")
    @Size(max = 255, message = "反馈标题长度不能超过255个字符")
    private String title;

    @NotBlank(message = "反馈内容不能为空")
    private String content;

    private List<FeedbackAttachmentItem> attachments;
    private Map<String, Object> extraInfo;
}
