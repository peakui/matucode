package com.peakui.message.collab.model.vo;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CollabRoomStateVO {
    private CollabDocumentVO document;
    private List<CollabOnlineUserVO> onlineUsers;
    private List<CollabOperationVO> recentOperations;
}
