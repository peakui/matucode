package com.peakui.search.model;

import java.util.Arrays;

public enum SearchType {
    ARTICLE("article", "/article/"),
    COURSE("course", "/detail/course/"),
    INTERVIEW("interview", "/detail/interview-question/"),
    OJ("oj", "/problem/");

    private final String value;
    private final String path;

    SearchType(String value, String path) {
        this.value = value;
        this.path = path;
    }

    public String value() { return value; }
    public String targetPath(String id) { return path + id; }

    public static SearchType parse(String value) {
        return Arrays.stream(values()).filter(type -> type.value.equals(value)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的搜索类型"));
    }
}
