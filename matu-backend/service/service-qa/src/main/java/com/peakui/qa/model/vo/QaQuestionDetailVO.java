package com.peakui.qa.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 问题详情。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QaQuestionDetailVO {

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
    private String content;
    private Integer bountyPoints;
    private Integer viewCount;
    private Integer answerCount;
    private Integer followCount;
    private Integer shareCount;
    private Integer status;
    private Long bestAnswerId;
    private Boolean followed;
    private LocalDateTime createdAt;
    private List<QaAnswerVO> answers;
}
