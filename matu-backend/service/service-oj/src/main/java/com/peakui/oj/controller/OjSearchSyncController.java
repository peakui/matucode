package com.peakui.oj.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.peakui.common.result.ApiResponse;
import com.peakui.common.search.SearchSourceItem;
import com.peakui.common.search.SearchSyncAccess;
import com.peakui.common.search.SearchSyncControllerSupport;
import com.peakui.oj.mapper.OjProblemMapper;
import com.peakui.oj.model.entity.OjProblem;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class OjSearchSyncController extends SearchSyncControllerSupport {
    private final OjProblemMapper problemMapper;

    @Value("${search.sync.token:}")
    private String syncToken;

    @GetMapping("/oj/classes/internal/search-documents")
    public ApiResponse<List<SearchSourceItem>> searchDocuments(
            @RequestHeader(value = "X-Search-Sync-Token", required = false) String token,
            @RequestParam(value = "afterId", defaultValue = "0") Long afterId,
            @RequestParam(value = "pageSize", defaultValue = "100") int pageSize) {
        SearchSyncAccess.requireToken(syncToken, token);
        requirePage(afterId, pageSize);
        // Keep the public problem list's category exclusion, including its SQL NULL semantics.
        List<OjProblem> problems = problemMapper.selectList(new QueryWrapper<OjProblem>()
                .select("id", "title", "difficulty", "status", "created_at")
                .gt("id", afterId).eq("status", 1).ne("category_id", 2)
                .orderByAsc("id").last("LIMIT " + pageSize));
        return ApiResponse.success(problems.stream().map(problem -> SearchSourceItem.builder()
                .id(problem.getId().toString()).title(problem.getTitle()).difficulty(problem.getDifficulty())
                .status(problem.getStatus()).createdAt(problem.getCreatedAt()).tags(List.of())
                .build()).toList());
    }
}
