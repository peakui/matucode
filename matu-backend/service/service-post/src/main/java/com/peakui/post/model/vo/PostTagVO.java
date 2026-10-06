package com.peakui.post.model.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 标签信息。
 */
@Data
@Builder
public class PostTagVO {

    private Long id;
    private String tagName;
    private String tagDesc;
    private Integer postCount;
    private Integer status;
}
