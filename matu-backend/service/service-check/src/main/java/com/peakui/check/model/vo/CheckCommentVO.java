package com.peakui.check.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论返回。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "打卡评论")
public class CheckCommentVO {

    private Long id;
    private Long checkId;
    private Long userId;
    private String username;
    private String userAvatar;
    private String schoolName;
    private String companyName;
    private String authorTitle;
    private Integer authorIsVip;
    private Long parentId;
    private Long replyToUserId;
    private String replyToUsername;
    private String content;
    private Integer likeCount;
    private Boolean liked;
    private LocalDateTime createdAt;
    private List<CheckCommentVO> children;
}
