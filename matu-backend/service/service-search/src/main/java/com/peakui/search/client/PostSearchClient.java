package com.peakui.search.client;

import com.peakui.common.result.ApiResponse;
import com.peakui.common.search.SearchSourceItem;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@FeignClient("service-post")
public interface PostSearchClient {
    @GetMapping("/posts/internal/search-documents")
    ApiResponse<List<SearchSourceItem>> list(@RequestParam("afterId") long afterId,
            @RequestParam("pageSize") int pageSize, @RequestHeader("X-Search-Sync-Token") String token);
}
