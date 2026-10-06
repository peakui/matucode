package com.peakui.file.controller;

import com.peakui.common.exception.CommonError;
import com.peakui.common.result.ApiResponse;
import com.peakui.common.result.PageResponse;
import com.peakui.file.model.dto.ChunkCompleteRequest;
import com.peakui.file.model.dto.ChunkInitRequest;
import com.peakui.file.model.dto.FileUploadRequest;
import com.peakui.file.model.vo.ChunkInitVO;
import com.peakui.file.model.vo.ChunkStatusVO;
import com.peakui.file.model.vo.ChunkUploadVO;
import com.peakui.file.model.vo.FileInfoVO;
import com.peakui.file.model.vo.FileSignedUrlVO;
import com.peakui.file.model.vo.StorageBucketVO;
import com.peakui.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Validated
@Tag(name = "文件接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/files")
public class FileController {

    private final FileService fileService;

    @Operation(summary = "上传文件")
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ApiResponse<FileInfoVO> uploadFile(@RequestPart("file") MultipartFile file,
                                              @Valid FileUploadRequest request) {
        return ApiResponse.success(CommonError.UPLOAD_SUCCESS.message(), fileService.uploadFile(file, request));
    }

    @Operation(summary = "初始化分片上传")
    @PostMapping("/chunk/init")
    public ApiResponse<ChunkInitVO> initChunkUpload(@Valid @RequestBody ChunkInitRequest request) {
        return ApiResponse.success(fileService.initChunkUpload(request));
    }

    @Operation(summary = "上传单个分片")
    @PostMapping(value = "/chunk/upload", consumes = "multipart/form-data")
    public ApiResponse<ChunkUploadVO> uploadChunk(@RequestParam Long fileId,
                                                  @RequestParam Integer chunkNo,
                                                  @RequestParam(required = false) String chunkMd5,
                                                  @RequestPart("file") MultipartFile file) {
        return ApiResponse.success(fileService.uploadChunk(fileId, chunkNo, chunkMd5, file));
    }

    @Operation(summary = "查询分片上传状态")
    @GetMapping("/chunk/status")
    public ApiResponse<ChunkStatusVO> getChunkStatus(@RequestParam Long fileId) {
        return ApiResponse.success(fileService.getChunkStatus(fileId));
    }

    @Operation(summary = "完成分片上传")
    @PostMapping("/chunk/complete")
    public ApiResponse<FileInfoVO> completeChunkUpload(@Valid @RequestBody ChunkCompleteRequest request) {
        return ApiResponse.success(fileService.completeChunkUpload(request));
    }

    @Operation(summary = "获取文件详情")
    @GetMapping("/{fileId}")
    public ApiResponse<FileInfoVO> getFileInfo(@PathVariable Long fileId) {
        return ApiResponse.success(fileService.getFileInfo(fileId));
    }

    @Operation(summary = "获取文件签名地址")
    @GetMapping("/{fileId}/download-url")
    public ApiResponse<FileSignedUrlVO> getSignedUrl(@PathVariable Long fileId) {
        return ApiResponse.success(fileService.getSignedUrl(fileId));
    }

    @Operation(summary = "删除文件")
    @DeleteMapping("/{fileId}")
    public ApiResponse<Void> deleteFile(@PathVariable Long fileId) {
        fileService.deleteFile(fileId);
        return ApiResponse.success(CommonError.DELETE_SUCCESS.message(), null);
    }

    @Operation(summary = "文件列表")
    @GetMapping
    public ApiResponse<PageResponse<FileInfoVO>> listFiles(@RequestParam(required = false) Long ownerId,
                                                           @RequestParam(required = false) Integer ownerType,
                                                           @RequestParam(required = false) String fileType,
                                                           @RequestParam(required = false) String bucketName,
                                                           @RequestParam(required = false) Integer status,
                                                           @RequestParam(defaultValue = "1") Long pageNum,
                                                           @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.success(fileService.listFiles(ownerId, ownerType, fileType, bucketName, status, pageNum, pageSize));
    }

    @Operation(summary = "存储桶列表")
    @GetMapping("/buckets")
    public ApiResponse<List<StorageBucketVO>> listBuckets() {
        return ApiResponse.success(fileService.listBuckets());
    }
}
