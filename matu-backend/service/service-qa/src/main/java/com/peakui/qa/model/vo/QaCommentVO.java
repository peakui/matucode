package com.peakui.qa.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论视图。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QaCommentVO {

    private Long id;
    private Integer targetType;
    private Long targetId;
    private Long userId;
    private String username;
    private String avatarUrl;
    private String schoolName;
    private String companyName;
    private String authorTitle;
    private Integer authorIsVip;
    private Long parentId;
    private Long replyToUserId;
    private String content;
    private Integer likeCount;
    private Boolean liked;
    private LocalDateTime createdAt;
    private List<QaCommentVO> children;
}
