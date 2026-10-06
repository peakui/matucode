package com.peakui.message.collab.model.vo;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CollabDocumentVO {
    private Long id;
    private Long conversationId;
    private String title;
    private String content;
    private Long revision;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
