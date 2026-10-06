package com.peakui.message.collab.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CollabEditPayload {
    @NotNull(message = "基础版本不能为空")
    @Min(value = 0, message = "基础版本不能小于0")
    private Long baseRevision;
    @Valid
    @NotNull(message = "编辑操作不能为空")
    private TextOperationDTO operation;
}
