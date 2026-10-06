package com.peakui.post.model.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 分类信息。
 */
@Data
@Builder
public class PostCategoryVO {

    private Long id;
    private Long parentId;
    private String categoryName;
    private String categoryDesc;
    private String iconUrl;
    private Integer sortOrder;
    private Integer postCount;
    private Integer status;
}
