package com.peakui.message.collab.model.vo;

import com.peakui.message.collab.model.dto.TextOperationDTO;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CollabOperationVO {
    private Long id;
    private Long documentId;
    private Long conversationId;
    private Long revision;
    private Long baseRevision;
    private Long userId;
    private String clientId;
    private String requestId;
    private TextOperationDTO operation;
    private TextOperationDTO transformedOperation;
    private LocalDateTime createdAt;
}
