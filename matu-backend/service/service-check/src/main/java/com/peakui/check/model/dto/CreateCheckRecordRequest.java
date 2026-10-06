package com.peakui.check.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 创建打卡请求。
 */
@Data
@Schema(description = "创建打卡请求")
public class CreateCheckRecordRequest {

    @Schema(description = "标题", example = "第15天算法学习打卡")
    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题长度不能超过100个字符")
    private String title;

    @Schema(description = "摘要", example = "今天完成了二叉树专题复习")
    @Size(max = 255, message = "摘要长度不能超过255个字符")
    private String summary;

    @Schema(description = "正文内容")
    @NotBlank(message = "打卡内容不能为空")
    private String content;

    @Schema(description = "图片 URL 列表")
    @Size(max = 9, message = "图片数量不能超过9张")
    private List<@NotBlank(message = "图片地址不能为空") @Size(max = 255, message = "图片地址长度不能超过255个字符") String> imageUrls;

    @Schema(description = "学习时长(小时)", example = "2.50")
    @NotNull(message = "学习时长不能为空")
    @DecimalMin(value = "0.00", message = "学习时长不能小于0")
    @DecimalMax(value = "24.00", message = "学习时长不能超过24小时")
    private BigDecimal learnHours;

    @Schema(description = "心情 1-5", example = "5")
    @Min(value = 1, message = "心情值不能小于1")
    @Max(value = 5, message = "心情值不能大于5")
    private Integer mood;

    @Schema(description = "打卡地点", example = "图书馆")
    @Size(max = 100, message = "地点长度不能超过100个字符")
    private String location;

    @Schema(description = "打卡日期", example = "2026-04-23")
    @NotNull(message = "打卡日期不能为空")
    private LocalDate checkDate;

    @Schema(description = "状态 1已发布 2草稿 4仅自己可见", example = "1")
    private Integer status;
}
