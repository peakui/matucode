package com.peakui.message.collab.model.dto;

import lombok.Data;

@Data
public class CollabPresencePayload {
    private Integer cursor;
    private Integer selectionStart;
    private Integer selectionEnd;
}
