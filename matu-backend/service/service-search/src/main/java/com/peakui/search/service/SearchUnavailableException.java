package com.peakui.search.service;

/** Infrastructure failure, distinct from an empty but healthy search index. */
public class SearchUnavailableException extends RuntimeException {
    public SearchUnavailableException(Throwable cause) {
        super("搜索服务暂不可用", cause);
    }
}
