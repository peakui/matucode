package com.peakui.search.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchPageResponse {
    private long pageNum;
    private long pageSize;
    private long total;
    private long totalPages;
    private List<SearchResultItem> records;
}
