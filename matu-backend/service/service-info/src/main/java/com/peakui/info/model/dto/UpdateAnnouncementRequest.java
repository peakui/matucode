package com.peakui.info.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateAnnouncementRequest {

    @NotBlank(message = "公告标题不能为空")
    @Size(max = 255, message = "公告标题长度不能超过255个字符")
    private String title;

    @NotBlank(message = "公告内容不能为空")
    private String content;

    @Min(value = 0, message = "公告类型不合法")
    @Max(value = 3, message = "公告类型不合法")
    private Integer type;

    private Integer priority;

    @Min(value = 0, message = "置顶状态不合法")
    @Max(value = 1, message = "置顶状态不合法")
    private Integer isPinned;

    private LocalDateTime publishTime;
    private LocalDateTime expireTime;

    @Min(value = 0, message = "公告状态不合法")
    @Max(value = 2, message = "公告状态不合法")
    private Integer status;
}
