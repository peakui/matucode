package com.peakui.interview.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peakui.common.result.ApiResponse;
import com.peakui.common.search.SearchSourceItem;
import com.peakui.common.search.SearchSyncAccess;
import com.peakui.common.search.SearchSyncControllerSupport;
import com.peakui.interview.mapper.InterviewCategoryMapper;
import com.peakui.interview.mapper.InterviewQuestionMapper;
import com.peakui.interview.model.entity.InterviewCategory;
import com.peakui.interview.model.entity.InterviewQuestion;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class InterviewSearchSyncController extends SearchSyncControllerSupport {
    private final InterviewQuestionMapper questionMapper;
    private final InterviewCategoryMapper categoryMapper;
    private final ObjectMapper objectMapper;

    @Value("${search.sync.token:}")
    private String syncToken;

    @GetMapping("/interview/internal/search-documents")
    public ApiResponse<List<SearchSourceItem>> searchDocuments(
            @RequestHeader(value = "X-Search-Sync-Token", required = false) String token,
            @RequestParam(value = "afterId", defaultValue = "0") Long afterId,
            @RequestParam(value = "pageSize", defaultValue = "100") int pageSize) {
        SearchSyncAccess.requireToken(syncToken, token);
        requirePage(afterId, pageSize);
        List<InterviewQuestion> questions = questionMapper.selectList(new QueryWrapper<InterviewQuestion>()
                .select("id", "title", "category_id", "position_tags", "difficulty", "status", "created_at")
                .gt("id", afterId).eq("status", 1).orderByAsc("id").last("LIMIT " + pageSize));
        if (questions.isEmpty()) return ApiResponse.success(List.of());
        List<Long> categoryIds = questions.stream().map(InterviewQuestion::getCategoryId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, String> categories = new java.util.LinkedHashMap<>();
        if (!categoryIds.isEmpty()) {
            categories.putAll(categoryMapper.selectList(new QueryWrapper<InterviewCategory>()
                            .select("id", "category_name").in("id", categoryIds))
                    .stream().collect(Collectors.toMap(InterviewCategory::getId, InterviewCategory::getCategoryName)));
        }
        return ApiResponse.success(questions.stream().map(question -> SearchSourceItem.builder()
                .id(question.getId().toString()).title(question.getTitle())
                .categoryName(categories.get(question.getCategoryId())).tags(parseTags(question.getPositionTags()))
                .difficulty(question.getDifficulty()).status(question.getStatus()).createdAt(question.getCreatedAt())
                .build()).toList());
    }

    private List<String> parseTags(String raw) {
        if (raw == null || raw.isBlank()) return Collections.emptyList();
        try {
            List<String> tags = objectMapper.readValue(raw, new TypeReference<>() {});
            return tags == null ? Collections.emptyList() : tags;
        } catch (com.fasterxml.jackson.core.JsonProcessingException ignored) {
            // Invalid legacy tags must not prevent a whole synchronization page from completing.
            return Collections.emptyList();
        }
    }
}
