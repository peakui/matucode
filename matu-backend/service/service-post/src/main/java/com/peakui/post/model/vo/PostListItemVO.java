package com.peakui.post.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 帖子列表项。
 */
@Data
@Builder
public class PostListItemVO {

    private Long id;
    private Long userId;
    private String authorName;
    private String authorAvatar;
    private String authorSchoolName;
    private String authorCompanyName;
    private String authorTitle;
    private Integer authorIsVip;
    private Boolean owner;
    private Long categoryId;
    private String categoryName;
    private String title;
    private String summary;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer collectCount;
    private Integer shareCount;
    private Integer status;
    private Integer isTop;
    private Integer isEssence;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
    private List<String> tags;
    private String coverImage;
}
