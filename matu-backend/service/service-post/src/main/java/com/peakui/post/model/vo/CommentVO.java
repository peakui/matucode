package com.peakui.post.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论信息。
 */
@Data
@Builder
public class CommentVO {

    private Long id;
    private Long postId;
    private Long userId;
    private Long parentId;
    private Long replyToUserId;
    private String content;
    private Integer likeCount;
    private Boolean liked;
    private Integer replyCount;
    private Integer status;
    private String ipAddress;
    private String userName;
    private String userAvatar;
    private String userSchoolName;
    private String userCompanyName;
    private String userTitle;
    private Integer authorIsVip;
    private Boolean owner;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CommentVO> replies;
}
