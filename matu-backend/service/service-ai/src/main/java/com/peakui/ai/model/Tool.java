package com.peakui.ai.model;

import com.fasterxml.jackson.databind.JsonNode;

/** An OpenAI-style tool definition advertised to the model via {@code tools[]}. */
public record Tool(String type, FunctionDefinition function) {

    public static Tool function(String name, String description, JsonNode parameters) {
        return new Tool("function", new FunctionDefinition(name, description, parameters));
    }

    public record FunctionDefinition(String name, String description, JsonNode parameters) {
    }
}
