package com.peakui.search.controller;

import com.peakui.common.result.ApiResponse;
import com.peakui.search.model.IndexStatusVO;
import com.peakui.search.model.SyncResultVO;
import com.peakui.search.service.SearchIndexSyncService;
import com.peakui.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "搜索管理接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/search/admin")
public class AdminSearchController {

    private final SearchService searchService;
    private final SearchIndexSyncService searchIndexSyncService;

    @Operation(summary = "索引状态")
    @GetMapping("/status")
    public ApiResponse<IndexStatusVO> status() {
        return ApiResponse.success(searchService.getIndexStatus());
    }

    @Operation(summary = "触发全量重建索引")
    @PostMapping("/reindex")
    public ApiResponse<SyncResultVO> reindex() {
        return ApiResponse.success(searchIndexSyncService.syncNow());
    }
}
