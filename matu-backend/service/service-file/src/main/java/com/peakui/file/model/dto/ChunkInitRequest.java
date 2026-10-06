package com.peakui.file.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChunkInitRequest {
    @NotBlank
    private String originalName;
    private String fileType;

    @NotNull
    @Min(1)
    private Long fileSize;

    private String fileMd5;

    @NotNull
    @Min(1)
    private Integer chunkSize;

    @NotNull
    @Min(1)
    private Integer chunkCount;

    private String bucketName;
    private Integer ownerType;
    private Integer isPublic;
    private LocalDateTime expiresAt;
}
