package com.peakui.post.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI 自动总结和评论的内部请求。
 */
@Data
public class ApplyAiSummaryCommentRequest {
    @NotBlank(message = "AI 总结不能为空")
    @Size(max = 500, message = "AI 总结不能超过500个字符")
    private String summary;

    @NotBlank(message = "AI 评论不能为空")
    @Size(max = 1000, message = "AI 评论不能超过1000个字符")
    private String comment;

    @NotBlank
    @jakarta.validation.constraints.Pattern(regexp = "[a-f0-9]{64}")
    private String sourceHash;
}
