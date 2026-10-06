package com.peakui.check.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建打卡小组请求。
 */
@Data
@Schema(description = "创建打卡小组请求")
public class CreateCheckGroupRequest {

    @Schema(description = "小组名称", example = "算法冲刺队")
    @NotBlank(message = "小组名称不能为空")
    @Size(max = 50, message = "小组名称长度不能超过50个字符")
    private String groupName;

    @Schema(description = "小组描述", example = "一起坚持100天算法打卡")
    @Size(max = 200, message = "小组描述长度不能超过200个字符")
    private String groupDesc;

    @Schema(description = "封面图", example = "https://cdn.example.com/check/group-cover.png")
    @Size(max = 255, message = "封面图长度不能超过255个字符")
    private String coverImage;

    @Schema(description = "是否公开 0私有 1公开", example = "1")
    private Integer isPublic;
}
