package com.peakui.message.model.dto;

import lombok.Data;

@Data
public class MarkConversationReadRequest {
    private Long lastReadMessageId;
}
