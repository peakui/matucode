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
public class SearchResultItem {
    private String id;
    private String type;
    private String title;
    private String summary;
    private List<String> tags;
    private String categoryName;
    private Integer difficulty;
    private String coverUrl;
    private String targetPath;
    private String publishedAt;
    private String createdAt;
}
