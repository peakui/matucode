package com.peakui.message.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendMessageRequest {
    @NotNull(message = "消息类型不能为空")
    private Integer messageType;
    private String content;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private Long replyToId;
}
