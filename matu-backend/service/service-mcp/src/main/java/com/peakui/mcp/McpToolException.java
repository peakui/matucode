package com.peakui.mcp;

/** A tool-level failure: reported to the client as {@code isError: true}, not a JSON-RPC error. */
public class McpToolException extends RuntimeException {

    public McpToolException(String message) {
        super(message);
    }

    public McpToolException(String message, Throwable cause) {
        super(message, cause);
    }
}
