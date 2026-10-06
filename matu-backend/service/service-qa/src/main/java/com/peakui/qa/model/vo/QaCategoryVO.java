package com.peakui.qa.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分类树视图。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QaCategoryVO {

    private Long id;
    private Long parentId;
    private String categoryName;
    private String categoryDesc;
    private String iconUrl;
    private Integer sortOrder;
    private Integer questionCount;
    private List<QaCategoryVO> children;
}
