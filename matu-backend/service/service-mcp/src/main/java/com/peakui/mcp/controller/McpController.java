package com.peakui.mcp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.peakui.mcp.mcp.McpDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Single JSON-RPC endpoint backing the MCP transport. Auth is enforced by
 * {@link com.peakui.mcp.security.McpTokenFilter} before the request reaches here.
 */
@RestController
@RequiredArgsConstructor
public class McpController {

    private final McpDispatcher dispatcher;

    @PostMapping(path = "/mcp", consumes = "application/json", produces = "application/json")
    public ResponseEntity<JsonNode> mcp(@RequestBody JsonNode request) {
        JsonNode response = dispatcher.dispatch(request);
        if (response == null) {
            return ResponseEntity.accepted().build();
        }
        return ResponseEntity.ok(response);
    }
}
