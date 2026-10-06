package com.peakui.post.model.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 图片信息。
 */
@Data
@Builder
public class PostImageVO {

    private Long id;
    private String imageUrl;
    private String imageType;
    private Integer sortOrder;
}
