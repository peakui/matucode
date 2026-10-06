package com.peakui.qa.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 回答视图。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QaAnswerVO {

    private Long id;
    private Long questionId;
    private Long userId;
    private String username;
    private String avatarUrl;
    private String schoolName;
    private String companyName;
    private String authorTitle;
    private Integer authorIsVip;
    private String content;
    private Integer likeCount;
    private Integer dislikeCount;
    private Integer isAccepted;
    private LocalDateTime createdAt;
}
