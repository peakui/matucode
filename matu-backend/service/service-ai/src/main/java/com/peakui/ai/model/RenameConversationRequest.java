package com.peakui.ai.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameConversationRequest(
        @NotBlank(message = "标题不能为空") @Size(max = 100, message = "标题最多 100 个字符") String title
) {
}
