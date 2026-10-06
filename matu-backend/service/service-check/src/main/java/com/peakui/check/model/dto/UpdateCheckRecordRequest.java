package com.peakui.check.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 更新打卡请求。
 */
@Data
@Schema(description = "更新打卡请求")
public class UpdateCheckRecordRequest {

    @Schema(description = "标题", example = "第15天算法学习打卡")
    @Size(max = 100, message = "标题长度不能超过100个字符")
    private String title;

    @Schema(description = "摘要", example = "今天完成了二叉树专题复习")
    @Size(max = 255, message = "摘要长度不能超过255个字符")
    private String summary;

    @Schema(description = "正文内容")
    private String content;

    @Schema(description = "图片 URL 列表")
    @Size(max = 9, message = "图片数量不能超过9张")
    private List<@Size(max = 255, message = "图片地址长度不能超过255个字符") String> imageUrls;

    @Schema(description = "学习时长(小时)", example = "2.50")
    private BigDecimal learnHours;

    @Schema(description = "心情 1-5", example = "5")
    private Integer mood;

    @Schema(description = "打卡地点", example = "图书馆")
    @Size(max = 100, message = "地点长度不能超过100个字符")
    private String location;

    @Schema(description = "打卡日期", example = "2026-04-23")
    private LocalDate checkDate;

    @Schema(description = "状态 1已发布 2草稿 4仅自己可见", example = "1")
    private Integer status;
}
