package com.peakui.message.collab.service;

import com.peakui.message.collab.model.dto.CollabEditPayload;
import com.peakui.message.collab.model.vo.CollabOperationVO;

public interface CollabOperationService {
    CollabOperationVO applyOperation(Long userId, Long conversationId, Long documentId, String clientId,
                                     String requestId, CollabEditPayload payload);
}
