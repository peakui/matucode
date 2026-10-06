package com.peakui.ai.model;

public record AiAdminStatusVO(
        boolean recordStoreAvailable,
        boolean mcpEnabled,
        int mcpServerCount,
        boolean ragEnabled,
        String chatModel,
        String embeddingModel
) {
}
