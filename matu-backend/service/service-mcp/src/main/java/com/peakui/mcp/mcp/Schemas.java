package com.peakui.mcp.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Small builder for the JSON Schema objects declared by each tool. */
public final class Schemas {

    private final ObjectNode schema;
    private final ObjectNode properties;
    private final ArrayNode required;

    private Schemas(ObjectMapper mapper) {
        this.schema = mapper.createObjectNode();
        this.schema.put("type", "object");
        this.properties = schema.putObject("properties");
        this.required = mapper.createArrayNode();
    }

    public static Schemas of(ObjectMapper mapper) {
        return new Schemas(mapper);
    }

    public Schemas string(String name, String description, boolean required) {
        properties.putObject(name).put("type", "string").put("description", description);
        return mark(name, required);
    }

    public Schemas integer(String name, String description, boolean required) {
        properties.putObject(name).put("type", "integer").put("description", description);
        return mark(name, required);
    }

    private Schemas mark(String name, boolean required) {
        if (required) {
            this.required.add(name);
        }
        return this;
    }

    public ObjectNode build() {
        if (!required.isEmpty()) {
            schema.set("required", required);
        }
        return schema;
    }
}
