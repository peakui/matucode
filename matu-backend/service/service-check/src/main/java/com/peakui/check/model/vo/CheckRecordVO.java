package com.peakui.check.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 打卡详情返回。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "打卡详情")
public class CheckRecordVO {

    private Long id;
    private Long userId;
    private String username;
    private String avatarUrl;
    private String schoolName;
    private String companyName;
    private String authorTitle;
    private Integer authorIsVip;
    private String title;
    private String summary;
    private String content;
    private List<String> imageUrls;
    private BigDecimal learnHours;
    private Integer mood;
    private String location;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer shareCount;
    private Integer isTop;
    private Integer isFeatured;
    private Integer status;
    private LocalDate checkDate;
    private LocalDateTime checkTime;
    private Boolean liked;
    private Boolean canEdit;
}
