package com.peakui.qa.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 问题列表项。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QaQuestionListItemVO {

    private Long id;
    private Long userId;
    private String username;
    private String avatarUrl;
    private String schoolName;
    private String companyName;
    private String authorTitle;
    private Integer authorIsVip;
    private Long categoryId;
    private String categoryName;
    private String title;
    private Integer bountyPoints;
    private Integer viewCount;
    private Integer answerCount;
    private Integer followCount;
    private Integer status;
    private LocalDateTime createdAt;
}
