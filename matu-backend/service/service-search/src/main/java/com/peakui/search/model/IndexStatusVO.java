package com.peakui.search.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexStatusVO {
    private String alias;
    private boolean indexExists;
    private long documentCount;
    private long maxDocuments;
}
