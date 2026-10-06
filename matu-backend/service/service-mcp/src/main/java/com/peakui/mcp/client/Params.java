package com.peakui.mcp.client;

import java.util.LinkedHashMap;
import java.util.Map;

/** Builds a query-param map that tolerates null values (unlike {@link Map#of}). */
public final class Params {

    private Params() {
    }

    public static Map<String, Object> of(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }
}
