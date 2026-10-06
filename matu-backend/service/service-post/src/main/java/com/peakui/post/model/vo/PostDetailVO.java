package com.peakui.post.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 帖子详情。
 */
@Data
@Builder
public class PostDetailVO {

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
    private Integer contentType;
    private String content;
    private Integer wordCount;
    private Integer readTime;
    private Integer version;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer collectCount;
    private Integer shareCount;
    private Integer status;
    private Integer isTop;
    private Integer isEssence;
    private Integer isLock;
    private String lockReason;
    private Boolean liked;
    private Boolean collected;
    private Integer topCommentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
    private List<PostImageVO> images;
    private List<String> tags;
    private List<CommentVO> recentComments;
}
