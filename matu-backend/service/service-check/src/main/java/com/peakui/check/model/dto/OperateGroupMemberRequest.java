package com.peakui.check.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 小组成员操作请求。
 */
@Data
@Schema(description = "小组成员操作请求")
public class OperateGroupMemberRequest {

    @NotNull(message = "成员用户ID不能为空")
    @Schema(description = "成员用户ID", example = "10001")
    private Long memberUserId;
}
