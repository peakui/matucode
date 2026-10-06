package com.peakui.mcp.mcp;

import com.peakui.mcp.McpToolException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ToolRegistry {

    private final Map<String, McpTool> tools;

    public ToolRegistry(List<McpTool> tools) {
        Map<String, McpTool> indexed = new LinkedHashMap<>();
        for (McpTool tool : tools) {
            McpTool previous = indexed.put(tool.name(), tool);
            if (previous != null) {
                throw new IllegalStateException("重复的 MCP 工具名: " + tool.name());
            }
        }
        this.tools = Map.copyOf(indexed);
    }

    public Collection<McpTool> all() {
        return tools.values();
    }

    public McpTool require(String name) {
        McpTool tool = tools.get(name);
        if (tool == null) {
            throw new McpToolException("未知工具: " + name);
        }
        return tool;
    }
}
