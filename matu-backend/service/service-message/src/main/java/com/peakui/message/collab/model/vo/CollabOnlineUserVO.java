package com.peakui.message.collab.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CollabOnlineUserVO {
    private Long userId;
    private String clientId;
    private Integer cursor;
    private Integer selectionStart;
    private Integer selectionEnd;
}
