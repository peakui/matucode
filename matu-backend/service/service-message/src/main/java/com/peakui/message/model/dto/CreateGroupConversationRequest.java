package com.peakui.message.model.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CreateGroupConversationRequest {
    private String conversationName;
    @NotEmpty(message = "群聊成员不能为空")
    private List<Long> memberIds;
}
