package com.peakui.search.service;

import com.peakui.common.result.ApiResponse;
import com.peakui.common.search.SearchSourceItem;
import com.peakui.search.client.CourseSearchClient;
import com.peakui.search.client.InterviewSearchClient;
import com.peakui.search.client.OjSearchClient;
import com.peakui.search.client.PostSearchClient;
import com.peakui.search.config.SearchProperties;
import com.peakui.search.model.SyncResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchIndexSyncService {
    private static final int PAGE_SIZE = 100;
    private final SearchService searchService;
    private final SearchProperties properties;
    private final PostSearchClient postSearchClient;
    private final CourseSearchClient courseSearchClient;
    private final InterviewSearchClient interviewSearchClient;
    private final OjSearchClient ojSearchClient;

    @Scheduled(initialDelayString = "${search.sync.initial-delay-ms:30000}", fixedDelayString = "${search.sync.fixed-delay-ms:1800000}")
    public void scheduledSync() {
        syncNow();
    }

    /**
     * Pulls every public document and swaps the index alias to a freshly built generation.
     * Returns the outcome rather than throwing, so the caller (and the admin console) can
     * report a soft failure while the previously built index keeps serving.
     */
    public SyncResultVO syncNow() {
        try {
            List<SearchSourceItem> documents = new ArrayList<>();
            collect(documents, postSearchClient, "article");
            collect(documents, courseSearchClient, "course");
            collect(documents, interviewSearchClient, "interview");
            collect(documents, ojSearchClient, "oj");
            if (documents.size() > properties.getSync().getMaxDocuments()) {
                throw new IllegalStateException("搜索文档数量超过安全上限");
            }
            // replaceDocuments creates a new mapped generation and switches the alias only after success.
            searchService.replaceDocuments(documents);
            log.info("搜索索引同步完成，写入 {} 条公开文档", documents.size());
            return new SyncResultVO(true, documents.size(), "索引重建完成");
        } catch (Exception exception) {
            log.warn("搜索索引同步失败，保留已有索引: {}", exception.getMessage());
            return new SyncResultVO(false, 0, exception.getMessage() == null ? "索引重建失败" : exception.getMessage());
        }
    }

    private void collect(List<SearchSourceItem> target, Object client, String type) {
        long afterId = 0;
        while (true) {
            ApiResponse<List<SearchSourceItem>> response = fetch(client, afterId);
            if (response == null || response.getCode() == null || response.getCode() != 0) {
                throw new IllegalStateException("同步端点返回失败: " + type);
            }
            List<SearchSourceItem> page = response.getData();
            if (page == null) throw new IllegalStateException("同步端点缺少数据: " + type);
            if (page.stream().anyMatch(item -> item == null || !Integer.valueOf(1).equals(item.getStatus()))) {
                throw new IllegalStateException("同步端点返回非公开文档: " + type);
            }
            List<SearchSourceItem> records = page.stream()
                    .map(item -> copyWithType(item, type)).toList();
            target.addAll(records);
            if (records.size() < PAGE_SIZE) break;
            long nextId = records.stream().map(SearchSourceItem::getId).map(this::parseId)
                    .max(Long::compareTo).orElse(afterId);
            if (nextId <= afterId) throw new IllegalStateException("同步端点分页游标未前进: " + type);
            afterId = nextId;
        }
    }

    private ApiResponse<List<SearchSourceItem>> fetch(Object client, long afterId) {
        String token = properties.getSync().getToken();
        if (client instanceof PostSearchClient item) return item.list(afterId, PAGE_SIZE, token);
        if (client instanceof CourseSearchClient item) return item.list(afterId, PAGE_SIZE, token);
        if (client instanceof InterviewSearchClient item) return item.list(afterId, PAGE_SIZE, token);
        return ((OjSearchClient) client).list(afterId, PAGE_SIZE, token);
    }

    private long parseId(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception exception) {
            throw new IllegalStateException("同步数据包含无效 ID", exception);
        }
    }

    private SearchSourceItem copyWithType(SearchSourceItem source, String type) {
        return SearchSourceItem.builder().id(source.getId()).type(type).title(source.getTitle()).summary(source.getSummary())
                .categoryName(source.getCategoryName()).coverUrl(source.getCoverUrl()).tags(source.getTags())
                .difficulty(source.getDifficulty()).status(source.getStatus()).publishedAt(source.getPublishedAt())
                .createdAt(source.getCreatedAt()).build();
    }
}
