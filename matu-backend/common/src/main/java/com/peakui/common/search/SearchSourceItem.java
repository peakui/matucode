package com.peakui.common.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/** Public metadata only; never include article bodies, answers or problem content. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchSourceItem {
    private String id;
    /** Source kind is assigned by the search sync adapter (article/course/interview/oj). */
    private String type;
    private String title;
    private String summary;
    private String categoryName;
    private String coverUrl;
    private List<String> tags;
    private Integer difficulty;
    private Integer status;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
}
