package com.peakui.ai.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RagDocumentRequest(
        String tenantId,
        @NotBlank @Size(max = 100000) String content,
        String title
) {
}
