package com.peakui.file.service;

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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService {

    FileInfoVO uploadFile(MultipartFile file, FileUploadRequest request);

    ChunkInitVO initChunkUpload(ChunkInitRequest request);

    ChunkUploadVO uploadChunk(Long fileId, Integer chunkNo, String chunkMd5, MultipartFile file);

    ChunkStatusVO getChunkStatus(Long fileId);

    FileInfoVO completeChunkUpload(ChunkCompleteRequest request);

    FileInfoVO getFileInfo(Long fileId);

    FileSignedUrlVO getSignedUrl(Long fileId);

    void deleteFile(Long fileId);

    PageResponse<FileInfoVO> listFiles(Long ownerId, Integer ownerType, String fileType, String bucketName, Integer status,
                                       Long pageNum, Long pageSize);

    List<StorageBucketVO> listBuckets();
}
