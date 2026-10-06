package com.peakui.qa.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建评论请求。
 */
@Data
public class CreateQaCommentRequest {

    @NotNull(message = "目标类型不能为空")
    private Integer targetType;

    @NotNull(message = "目标ID不能为空")
    private Long targetId;

    private Long parentId;

    private Long replyToUserId;

    @NotBlank(message = "评论内容不能为空")
    private String content;
}
