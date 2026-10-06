package com.peakui.message.collab.model.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class CollabWsInboundMessage {
    private String type;
    private String requestId;
    private Long conversationId;
    private Long documentId;
    private String clientId;
    private JsonNode payload;
}
