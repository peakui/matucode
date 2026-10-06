package com.peakui.file.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChunkCompleteRequest {
    @NotNull
    private Long fileId;
}
