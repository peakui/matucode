package com.peakui.check.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 评论请求。
 */
@Data
@Schema(description = "发表评论请求")
public class CreateCheckCommentRequest {

    @Schema(description = "父评论ID", example = "0")
    private Long parentId;

    @Schema(description = "被回复用户ID", example = "10001")
    private Long replyToUserId;

    @Schema(description = "评论内容", example = "今天的打卡内容很充实")
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论内容长度不能超过500个字符")
    private String content;
}
