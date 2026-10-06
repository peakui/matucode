package com.peakui.course.feign;

import com.peakui.common.result.ApiResponse;
import com.peakui.course.feign.dto.ChunkCompleteRequest;
import com.peakui.course.feign.dto.ChunkInitRequest;
import com.peakui.course.feign.vo.ChunkInitVO;
import com.peakui.course.feign.vo.FileInfoVO;
import com.peakui.course.feign.vo.FileSignedUrlVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "service-file", path = "/files")
public interface FileFeignClient {
    @PostMapping("/chunk/init")
    ApiResponse<ChunkInitVO> initChunkUpload(@RequestBody ChunkInitRequest request);

    @PostMapping("/chunk/complete")
    ApiResponse<FileInfoVO> completeChunkUpload(@RequestBody ChunkCompleteRequest request);

    @GetMapping("/{fileId}")
    ApiResponse<FileInfoVO> getFileInfo(@PathVariable("fileId") Long fileId);

    @GetMapping("/{fileId}/download-url")
    ApiResponse<FileSignedUrlVO> getSignedUrl(@PathVariable("fileId") Long fileId);
}
