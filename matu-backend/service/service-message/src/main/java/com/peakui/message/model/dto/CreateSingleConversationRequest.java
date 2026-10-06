package com.peakui.message.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateSingleConversationRequest {
    @NotNull(message = "目标用户ID不能为空")
    private Long targetUserId;
}
