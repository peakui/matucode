package com.peakui.qa.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 词云视图。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QaWordCloudVO {

    private Long id;
    private Long questionId;
    private String wordData;
    private String imageUrl;
    private LocalDateTime generatedAt;
}
