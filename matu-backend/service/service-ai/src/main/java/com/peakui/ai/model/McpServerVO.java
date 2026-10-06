package com.peakui.ai.model;

import java.util.List;

public record McpServerVO(
        String name,
        String url,
        boolean enabled,
        List<String> allowTools,
        int allowToolCount
) {
}
