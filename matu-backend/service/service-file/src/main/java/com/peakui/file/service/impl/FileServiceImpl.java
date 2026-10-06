package com.peakui.file.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.peakui.common.result.PageResponse;
import com.peakui.file.config.FileProperties;
import com.peakui.file.exception.FileException;
import com.peakui.file.mapper.FileChunkMapper;
import com.peakui.file.mapper.FileInfoMapper;
import com.peakui.file.mapper.FileOperationMapper;
import com.peakui.file.mapper.StorageBucketMapper;
import com.peakui.file.model.dto.ChunkCompleteRequest;
import com.peakui.file.model.dto.ChunkInitRequest;
import com.peakui.file.model.dto.FileUploadRequest;
import com.peakui.file.model.entity.FileInfo;
import com.peakui.file.model.entity.StorageBucket;
import com.peakui.file.model.vo.ChunkInitVO;
import com.peakui.file.model.vo.ChunkStatusVO;
import com.peakui.file.model.vo.ChunkUploadVO;
import com.peakui.file.model.vo.FileInfoVO;
import com.peakui.file.model.vo.FileSignedUrlVO;
import com.peakui.file.model.vo.StorageBucketVO;
import com.peakui.file.service.FileService;
import com.peakui.file.util.LoginUserUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final int STATUS_UPLOADING = 0;
    private static final int STATUS_NORMAL = 1;
    private static final int STATUS_DELETED = 3;
    private static final int CHUNK_NOT_UPLOADED = 0;
    private static final int CHUNK_UPLOADED = 1;
    private static final long FILE_CACHE_TTL_SECONDS = 600;
    private static final String FILE_CACHE_KEY_PREFIX = "file:info:";

    // 扩展名 -> 服务端判定的 MIME。绝不回显客户端传入的 Content-Type：否则 .jpg 被标成
    // text/html 就能在公开域名上触发存储型 XSS。未列出的扩展名一律按二进制流下发。
    private static final Map<String, String> CONTENT_TYPES = Map.ofEntries(
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"),
            Map.entry("gif", "image/gif"),
            Map.entry("webp", "image/webp"),
            Map.entry("bmp", "image/bmp"),
            Map.entry("pdf", "application/pdf"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("zip", "application/zip"),
            Map.entry("rar", "application/vnd.rar"),
            Map.entry("txt", "text/plain; charset=UTF-8"),
            Map.entry("mp4", "video/mp4"),
            Map.entry("mov", "video/quicktime"),
            Map.entry("avi", "video/x-msvideo"),
            Map.entry("mkv", "video/x-matroska"));
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final FileInfoMapper fileInfoMapper;
    private final FileChunkMapper fileChunkMapper;
    private final StorageBucketMapper storageBucketMapper;
    private final FileOperationMapper fileOperationMapper;
    private final FileProperties fileProperties;
    private final LoginUserUtil loginUserUtil;
    private final HttpServletRequest request;
    private final OSS ossClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<Long, LocalCacheEntry> localFileCache = new ConcurrentHashMap<>();

    private record LocalCacheEntry(FileInfo fileInfo, long expiresAtMillis) {
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileInfoVO uploadFile(MultipartFile file, FileUploadRequest request) {
        validateMultipart(file);
        validateSize(file.getSize(), fileProperties.getFileMaxSizeBytes(), "文件大小超过限制");
        String extension = validateExtension(file.getOriginalFilename(), fileProperties.fileExtensionList(), "不支持的文件类型");
        String bucketName = resolveBucketName(request == null ? null : request.getBucketName());
        Integer ownerType = request != null && request.getOwnerType() != null ? request.getOwnerType() : 1;
        Long ownerId = loginUserUtil.getCurrentUserId();
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new FileException("读取上传文件失败");
        }
        String fileMd5 = request != null && StringUtils.hasText(request.getFileMd5())
                ? request.getFileMd5().trim()
                : org.springframework.util.DigestUtils.md5DigestAsHex(bytes);

        FileInfo existingFile = findNormalFile(ownerId, ownerType, bucketName, file.getSize(), fileMd5);
        if (existingFile != null) {
            return toFileInfoVO(existingFile);
        }

        String objectKey = buildObjectKey("file", extension);
        String contentType = resolveContentType(extension);
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(bytes.length);
            metadata.setContentType(contentType);
            metadata.setCacheControl("public, max-age=31536000, immutable");
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectKey, new ByteArrayInputStream(bytes));
            putObjectRequest.setMetadata(metadata);
            ossClient.putObject(putObjectRequest);
        } catch (Exception e) {
            log.error("OSS 上传失败 bucket={} key={} size={}", bucketName, objectKey, bytes.length, e);
            throw new FileException("文件上传失败");
        }

        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileName(extractFileName(objectKey));
        fileInfo.setOriginalName(file.getOriginalFilename());
        fileInfo.setFilePath(objectKey);
        fileInfo.setFileUrl(buildPublicUrl(bucketName, objectKey));
        fileInfo.setFileType(contentType);
        fileInfo.setFileSize(file.getSize());
        fileInfo.setFileMd5(fileMd5);
        fileInfo.setBucketName(bucketName);
        fileInfo.setOwnerId(ownerId);
        fileInfo.setOwnerType(ownerType);
        fileInfo.setIsPublic(request != null && request.getIsPublic() != null ? request.getIsPublic() : 0);
        fileInfo.setDownloadCount(0);
        fileInfo.setStatus(STATUS_NORMAL);
        fileInfo.setCreatedAt(LocalDateTime.now());
        fileInfo.setExpiresAt(request == null ? null : request.getExpiresAt());
        fileInfoMapper.insert(fileInfo);
        cacheFileInfo(fileInfo);
        return toFileInfoVO(fileInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChunkInitVO initChunkUpload(ChunkInitRequest request) {
        String bucketName = resolveBucketName(request.getBucketName());
        Long ownerId = loginUserUtil.getCurrentUserId();
        Integer ownerType = request.getOwnerType() == null ? 1 : request.getOwnerType();
        FileInfo existingFile = getResumableFile(request, bucketName, ownerId, ownerType);
        if (existingFile != null) {
            return buildChunkInitVO(existingFile, request.getChunkSize());
        }

        String chunkExtension = validateExtension(request.getOriginalName(), fileProperties.fileExtensionList(), "不支持的文件类型");
        String chunkContentType = resolveContentType(chunkExtension);
        String objectKey = buildObjectKey("image", chunkExtension);
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentType(chunkContentType);
        objectMetadata.setCacheControl("public, max-age=31536000, immutable");
        com.aliyun.oss.model.InitiateMultipartUploadRequest initiateRequest =
                new com.aliyun.oss.model.InitiateMultipartUploadRequest(bucketName, objectKey, objectMetadata);
        String uploadId = ossClient.initiateMultipartUpload(initiateRequest).getUploadId();

        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileName(extractFileName(objectKey));
        fileInfo.setOriginalName(request.getOriginalName());
        fileInfo.setFilePath(objectKey);
        fileInfo.setFileUrl(buildPublicUrl(bucketName, objectKey));
        fileInfo.setFileType(chunkContentType);
        fileInfo.setFileSize(request.getFileSize());
        fileInfo.setFileMd5(request.getFileMd5());
        fileInfo.setBucketName(bucketName);
        fileInfo.setOwnerId(ownerId);
        fileInfo.setOwnerType(ownerType);
        fileInfo.setIsPublic(request.getIsPublic() == null ? 0 : request.getIsPublic());
        fileInfo.setDownloadCount(0);
        fileInfo.setStatus(STATUS_UPLOADING);
        fileInfo.setUploadId(uploadId);
        fileInfo.setCreatedAt(LocalDateTime.now());
        fileInfo.setExpiresAt(request.getExpiresAt());
        fileInfoMapper.insert(fileInfo);

        for (int i = 1; i <= request.getChunkCount(); i++) {
            var chunk = new com.peakui.file.model.entity.FileChunk();
            chunk.setFileId(fileInfo.getId());
            chunk.setChunkNo(i);
            chunk.setChunkSize(request.getChunkSize());
            chunk.setUploadStatus(CHUNK_NOT_UPLOADED);
            chunk.setCreatedAt(LocalDateTime.now());
            fileChunkMapper.insert(chunk);
        }

        return buildChunkInitVO(fileInfo, request.getChunkSize());
    }

    private FileInfo getResumableFile(ChunkInitRequest request, String bucketName, Long ownerId, Integer ownerType) {
        if (!StringUtils.hasText(request.getFileMd5())) {
            return null;
        }
        FileInfo fileInfo = fileInfoMapper.selectOne(new LambdaQueryWrapper<FileInfo>()
                .eq(FileInfo::getOwnerId, ownerId)
                .eq(FileInfo::getOwnerType, ownerType)
                .eq(FileInfo::getBucketName, bucketName)
                .eq(FileInfo::getFileMd5, request.getFileMd5().trim())
                .eq(FileInfo::getFileSize, request.getFileSize())
                .in(FileInfo::getStatus, List.of(STATUS_UPLOADING, STATUS_NORMAL))
                .orderByDesc(FileInfo::getCreatedAt)
                .last("limit 1"));
        if (fileInfo == null || Objects.equals(fileInfo.getStatus(), STATUS_NORMAL)) {
            return fileInfo;
        }
        List<com.peakui.file.model.entity.FileChunk> chunks = loadChunks(fileInfo.getId());
        boolean sameChunkConfig = chunks.size() == request.getChunkCount()
                && chunks.stream().allMatch(chunk -> Objects.equals(chunk.getChunkSize(), request.getChunkSize()));
        return sameChunkConfig ? fileInfo : null;
    }

    private FileInfo findNormalFile(Long ownerId, Integer ownerType, String bucketName, long fileSize, String fileMd5) {
        if (!StringUtils.hasText(fileMd5)) {
            return null;
        }
        return fileInfoMapper.selectOne(new LambdaQueryWrapper<FileInfo>()
                .eq(FileInfo::getOwnerId, ownerId)
                .eq(FileInfo::getOwnerType, ownerType)
                .eq(FileInfo::getBucketName, bucketName)
                .eq(FileInfo::getFileSize, fileSize)
                .eq(FileInfo::getFileMd5, fileMd5)
                .eq(FileInfo::getStatus, STATUS_NORMAL)
                .orderByDesc(FileInfo::getCreatedAt)
                .last("limit 1"));
    }

    private ChunkInitVO buildChunkInitVO(FileInfo fileInfo, Integer chunkSize) {
        List<com.peakui.file.model.entity.FileChunk> chunks = loadChunks(fileInfo.getId());
        List<Integer> uploadedChunks = chunks.stream()
                .filter(item -> Objects.equals(item.getUploadStatus(), CHUNK_UPLOADED))
                .map(com.peakui.file.model.entity.FileChunk::getChunkNo)
                .toList();
        boolean completed = Objects.equals(fileInfo.getStatus(), STATUS_NORMAL);
        return ChunkInitVO.builder()
                .fileId(fileInfo.getId())
                .uploadId(fileInfo.getUploadId())
                .chunkCount(chunks.size())
                .chunkSize(chunkSize)
                .uploadedChunks(uploadedChunks)
                .completed(completed)
                .fileUrl(completed ? fileInfo.getFileUrl() : null)
                .build();
    }

    private List<com.peakui.file.model.entity.FileChunk> loadChunks(Long fileId) {
        return fileChunkMapper.selectList(new LambdaQueryWrapper<com.peakui.file.model.entity.FileChunk>()
                .eq(com.peakui.file.model.entity.FileChunk::getFileId, fileId)
                .orderByAsc(com.peakui.file.model.entity.FileChunk::getChunkNo));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChunkUploadVO uploadChunk(Long fileId, Integer chunkNo, String chunkMd5, MultipartFile file) {
        validateMultipart(file);
        FileInfo fileInfo = getOwnedFile(fileId);
        var chunk = fileChunkMapper.selectOne(new LambdaQueryWrapper<com.peakui.file.model.entity.FileChunk>()
                .eq(com.peakui.file.model.entity.FileChunk::getFileId, fileId)
                .eq(com.peakui.file.model.entity.FileChunk::getChunkNo, chunkNo)
                .last("limit 1"));
        if (chunk == null) {
            throw new FileException("分片不存在");
        }
        try {
            byte[] bytes = file.getBytes();
            var uploadPartRequest = new com.aliyun.oss.model.UploadPartRequest();
            uploadPartRequest.setBucketName(fileInfo.getBucketName());
            uploadPartRequest.setKey(fileInfo.getFilePath());
            uploadPartRequest.setUploadId(fileInfo.getUploadId());
            uploadPartRequest.setInputStream(new ByteArrayInputStream(bytes));
            uploadPartRequest.setPartSize(bytes.length);
            uploadPartRequest.setPartNumber(chunkNo);
            String eTag = ossClient.uploadPart(uploadPartRequest).getETag();
            chunk.setChunkMd5(eTag);
            chunk.setChunkPath(fileInfo.getFilePath());
            chunk.setChunkSize(bytes.length);
            chunk.setUploadStatus(CHUNK_UPLOADED);
            fileChunkMapper.updateById(chunk);
        } catch (Exception e) {
            log.error("OSS 分片上传失败 fileId={} chunkNo={} size={}", fileId, chunkNo, file.getSize(), e);
            throw new FileException("上传分片失败");
        }
        return ChunkUploadVO.builder().fileId(fileId).chunkNo(chunkNo).uploadStatus(CHUNK_UPLOADED).build();
    }

    @Override
    public ChunkStatusVO getChunkStatus(Long fileId) {
        FileInfo fileInfo = getOwnedFile(fileId);
        List<com.peakui.file.model.entity.FileChunk> chunks = loadChunks(fileId);
        List<Integer> uploadedChunks = chunks.stream()
                .filter(item -> Objects.equals(item.getUploadStatus(), CHUNK_UPLOADED))
                .map(com.peakui.file.model.entity.FileChunk::getChunkNo)
                .toList();
        return ChunkStatusVO.builder()
                .fileId(fileId)
                .uploadId(fileInfo.getUploadId())
                .chunkCount(chunks.size())
                .uploadedChunks(uploadedChunks)
                .completed(chunks.stream().allMatch(item -> Objects.equals(item.getUploadStatus(), CHUNK_UPLOADED)))
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileInfoVO completeChunkUpload(ChunkCompleteRequest request) {
        FileInfo fileInfo = getOwnedFile(request.getFileId());
        List<com.peakui.file.model.entity.FileChunk> chunks = fileChunkMapper.selectList(new LambdaQueryWrapper<com.peakui.file.model.entity.FileChunk>()
                .eq(com.peakui.file.model.entity.FileChunk::getFileId, request.getFileId())
                .orderByAsc(com.peakui.file.model.entity.FileChunk::getChunkNo));
        if (chunks.isEmpty() || chunks.stream().anyMatch(item -> !Objects.equals(item.getUploadStatus(), CHUNK_UPLOADED))) {
            throw new FileException("仍有分片未上传完成");
        }
        List<com.aliyun.oss.model.PartETag> partETags = new ArrayList<>();
        for (com.peakui.file.model.entity.FileChunk chunk : chunks) {
            partETags.add(new com.aliyun.oss.model.PartETag(chunk.getChunkNo(), chunk.getChunkMd5()));
        }
        partETags.sort(Comparator.comparingInt(com.aliyun.oss.model.PartETag::getPartNumber));
        ossClient.completeMultipartUpload(new com.aliyun.oss.model.CompleteMultipartUploadRequest(
                fileInfo.getBucketName(), fileInfo.getFilePath(), fileInfo.getUploadId(), partETags
        ));
        fileInfo.setStatus(STATUS_NORMAL);
        fileInfoMapper.updateById(fileInfo);
        cacheFileInfo(fileInfo);
        return toFileInfoVO(fileInfo);
    }

    @Override
    public FileInfoVO getFileInfo(Long fileId) {
        return toFileInfoVO(getReadableFile(fileId));
    }

    @Override
    public FileSignedUrlVO getSignedUrl(Long fileId) {
        FileInfo fileInfo = getReadableFile(fileId);
        var request = new com.aliyun.oss.model.GeneratePresignedUrlRequest(
                fileInfo.getBucketName(),
                fileInfo.getFilePath(),
                com.aliyun.oss.HttpMethod.GET
        );
        request.setExpiration(new java.util.Date(System.currentTimeMillis() + fileProperties.getSignedUrlExpireSeconds() * 1000));
        var url = ossClient.generatePresignedUrl(request);
        fileInfo.setDownloadCount((fileInfo.getDownloadCount() == null ? 0 : fileInfo.getDownloadCount()) + 1);
        fileInfoMapper.updateById(fileInfo);
        return FileSignedUrlVO.builder()
                .fileId(fileId)
                .signedUrl(url == null ? null : url.toString())
                .expireSeconds(fileProperties.getSignedUrlExpireSeconds())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(Long fileId) {
        FileInfo fileInfo = getOwnedFile(fileId);
        ossClient.deleteObject(fileInfo.getBucketName(), fileInfo.getFilePath());
        fileInfo.setStatus(STATUS_DELETED);
        fileInfoMapper.updateById(fileInfo);
        evictFileCache(fileId);
    }

    @Override
    public PageResponse<FileInfoVO> listFiles(Long ownerId, Integer ownerType, String fileType, String bucketName, Integer status,
                                              Long pageNum, Long pageSize) {
        long currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long currentSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<FileInfo>()
                .orderByDesc(FileInfo::getCreatedAt);
        if (ownerId != null) {
            wrapper.eq(FileInfo::getOwnerId, ownerId);
        } else if (!isAdmin()) {
            wrapper.eq(FileInfo::getOwnerId, loginUserUtil.getCurrentUserId());
        }
        // Admins omitting ownerId get every owner's files; others keep seeing only their own.
        if (ownerType != null) {
            wrapper.eq(FileInfo::getOwnerType, ownerType);
        }
        if (StringUtils.hasText(fileType)) {
            wrapper.like(FileInfo::getFileType, fileType.trim());
        }
        if (StringUtils.hasText(bucketName)) {
            wrapper.eq(FileInfo::getBucketName, bucketName.trim());
        }
        if (status != null) {
            wrapper.eq(FileInfo::getStatus, status);
        } else {
            wrapper.ne(FileInfo::getStatus, STATUS_DELETED);
        }
        Page<FileInfo> page = fileInfoMapper.selectPage(new Page<>(currentPage, currentSize), wrapper);
        return PageResponse.of(currentPage, currentSize, page.getTotal(), page.getRecords().stream().map(this::toFileInfoVO).toList());
    }

    @Override
    public List<StorageBucketVO> listBuckets() {
        return storageBucketMapper.selectList(new LambdaQueryWrapper<StorageBucket>()
                        .eq(StorageBucket::getStatus, 1)
                        .orderByAsc(StorageBucket::getBucketName))
                .stream()
                .map(bucket -> StorageBucketVO.builder()
                        .id(bucket.getId())
                        .bucketName(bucket.getBucketName())
                        .bucketType(bucket.getBucketType())
                        .storageProvider(bucket.getStorageProvider())
                        .endpoint(bucket.getEndpoint())
                        .region(bucket.getRegion())
                        .maxSize(bucket.getMaxSize())
                        .usedSize(bucket.getUsedSize())
                        .fileCount(bucket.getFileCount())
                        .status(bucket.getStatus())
                        .build())
                .toList();
    }

    @Scheduled(fixedDelay = 21600000)
    public void cleanupExpiredAndAbandonedFiles() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime abandonedBefore = now.minus(24, ChronoUnit.HOURS);
        List<FileInfo> candidates = fileInfoMapper.selectList(new LambdaQueryWrapper<FileInfo>()
                .and(wrapper -> wrapper
                        .and(item -> item.eq(FileInfo::getStatus, STATUS_UPLOADING)
                                .lt(FileInfo::getCreatedAt, abandonedBefore))
                        .or(item -> item.eq(FileInfo::getStatus, STATUS_NORMAL)
                                .isNotNull(FileInfo::getExpiresAt)
                                .le(FileInfo::getExpiresAt, now))));
        for (FileInfo fileInfo : candidates) {
            try {
                if (Objects.equals(fileInfo.getStatus(), STATUS_UPLOADING) && StringUtils.hasText(fileInfo.getUploadId())) {
                    ossClient.abortMultipartUpload(new com.aliyun.oss.model.AbortMultipartUploadRequest(
                            fileInfo.getBucketName(), fileInfo.getFilePath(), fileInfo.getUploadId()));
                } else {
                    ossClient.deleteObject(fileInfo.getBucketName(), fileInfo.getFilePath());
                }
                fileInfo.setStatus(STATUS_DELETED);
                fileInfoMapper.updateById(fileInfo);
                evictFileCache(fileInfo.getId());
            } catch (Exception ignored) {
                // 单个对象清理失败不阻塞下一轮清理。
            }
        }
    }

    private FileInfo getReadableFile(Long fileId) {
        FileInfo fileInfo = loadCachedFileInfo(fileId);
        if (fileInfo == null) {
            fileInfo = fileInfoMapper.selectById(fileId);
            if (fileInfo != null) {
                cacheFileInfo(fileInfo);
            }
        }
        if (fileInfo == null || Objects.equals(fileInfo.getStatus(), STATUS_DELETED)) {
            throw new FileException("文件不存在");
        }
        Long currentUserId = loginUserUtil.getCurrentUserId();
        if (!Objects.equals(fileInfo.getIsPublic(), 1)
                && !Objects.equals(fileInfo.getOwnerId(), currentUserId)
                && !isAdmin()) {
            throw new FileException("无权访问该文件");
        }
        return fileInfo;
    }

    private FileInfo loadCachedFileInfo(Long fileId) {
        LocalCacheEntry local = localFileCache.get(fileId);
        if (local != null) {
            if (local.expiresAtMillis() > System.currentTimeMillis()) {
                return local.fileInfo();
            }
            localFileCache.remove(fileId, local);
        }
        try {
            String value = stringRedisTemplate.opsForValue().get(FILE_CACHE_KEY_PREFIX + fileId);
            if (StringUtils.hasText(value)) {
                FileInfo fileInfo = objectMapper.readValue(value, FileInfo.class);
                localFileCache.put(fileId, new LocalCacheEntry(fileInfo, System.currentTimeMillis() + FILE_CACHE_TTL_SECONDS * 1000));
                return fileInfo;
            }
        } catch (Exception ignored) {
            // Redis 不可用时继续回源数据库，保证文件查询可用。
        }
        return null;
    }

    private void cacheFileInfo(FileInfo fileInfo) {
        if (fileInfo == null || fileInfo.getId() == null) {
            return;
        }
        localFileCache.put(fileInfo.getId(), new LocalCacheEntry(fileInfo, System.currentTimeMillis() + FILE_CACHE_TTL_SECONDS * 1000));
        try {
            String value = objectMapper.writeValueAsString(fileInfo);
            stringRedisTemplate.opsForValue().set(FILE_CACHE_KEY_PREFIX + fileInfo.getId(), value, Duration.ofSeconds(FILE_CACHE_TTL_SECONDS));
        } catch (Exception ignored) {
            // 本地缓存仍可继续提供短期保护。
        }
    }

    private void evictFileCache(Long fileId) {
        localFileCache.remove(fileId);
        try {
            stringRedisTemplate.delete(FILE_CACHE_KEY_PREFIX + fileId);
        } catch (Exception ignored) {
            // 缓存失效失败不影响数据库删除结果。
        }
    }

    private FileInfo getOwnedFile(Long fileId) {
        FileInfo fileInfo = fileInfoMapper.selectById(fileId);
        if (fileInfo == null || Objects.equals(fileInfo.getStatus(), STATUS_DELETED)) {
            throw new FileException("文件不存在");
        }
        if (!Objects.equals(fileInfo.getOwnerId(), loginUserUtil.getCurrentUserId()) && !isAdmin()) {
            throw new FileException("无权操作该文件");
        }
        return fileInfo;
    }

    private FileInfoVO toFileInfoVO(FileInfo fileInfo) {
        return FileInfoVO.builder()
                .id(fileInfo.getId())
                .fileName(fileInfo.getFileName())
                .originalName(fileInfo.getOriginalName())
                .filePath(fileInfo.getFilePath())
                .fileUrl(fileInfo.getFileUrl())
                .fileType(fileInfo.getFileType())
                .fileSize(fileInfo.getFileSize())
                .fileMd5(fileInfo.getFileMd5())
                .bucketName(fileInfo.getBucketName())
                .ownerId(fileInfo.getOwnerId())
                .ownerType(fileInfo.getOwnerType())
                .isPublic(fileInfo.getIsPublic())
                .downloadCount(fileInfo.getDownloadCount())
                .status(fileInfo.getStatus())
                .uploadId(fileInfo.getUploadId())
                .createdAt(fileInfo.getCreatedAt())
                .expiresAt(fileInfo.getExpiresAt())
                .build();
    }

    private void validateMultipart(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileException("上传文件不能为空");
        }
        if (!StringUtils.hasText(file.getOriginalFilename())) {
            throw new FileException("文件名不能为空");
        }
    }

    private void validateSize(long fileSize, long maxSize, String message) {
        if (maxSize > 0 && fileSize > maxSize) {
            throw new FileException(message);
        }
    }

    private String getExtension(String filename) {
        int index = filename.lastIndexOf('.');
        return index < 0 || index == filename.length() - 1 ? "bin" : filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    /** 校验扩展名是否在白名单内，返回归一化后的扩展名；白名单为空时跳过（避免误伤）。 */
    private String validateExtension(String filename, List<String> allowed, String message) {
        String extension = getExtension(filename);
        if (!allowed.isEmpty() && !allowed.contains(extension)) {
            throw new FileException(message);
        }
        return extension;
    }

    private String resolveContentType(String extension) {
        return CONTENT_TYPES.getOrDefault(extension.toLowerCase(Locale.ROOT), DEFAULT_CONTENT_TYPE);
    }

    private String buildObjectKey(String directory, String extension) {
        LocalDateTime now = LocalDateTime.now();
        return directory + "/" + now.getYear() + "/" + now.getMonthValue() + "/" + now.getDayOfMonth() + "/" + UUID.randomUUID().toString().replace("-", "") + "." + extension;
    }

    private String extractFileName(String objectKey) {
        int index = objectKey.lastIndexOf('/');
        return index >= 0 ? objectKey.substring(index + 1) : objectKey;
    }

    /**
     * 只信任服务端配置的存储桶。客户端传入的 bucketName 曾是隐患：admin 把
     * "course-cover" 这类目录名当桶名，导致 OSS NoSuchBucket；同时它也让客户端
     * 能指定写入任意桶。现在一律使用配置的桶，仅在不一致时记一条告警便于排查。
     */
    private String resolveBucketName(String clientBucketName) {
        String configured = fileProperties.getOss().getBucketName();
        if (!StringUtils.hasText(configured)) {
            throw new FileException("存储桶未配置");
        }
        if (StringUtils.hasText(clientBucketName) && !configured.equals(clientBucketName.trim())) {
            log.warn("忽略客户端传入的 bucketName={}，改用配置的桶 {}", clientBucketName, configured);
        }
        return configured;
    }

    private String buildPublicUrl(String bucketName, String objectKey) {
        if (StringUtils.hasText(fileProperties.getOss().getPublicDomain())) {
            return fileProperties.getOss().getPublicDomain() + "/" + objectKey;
        }
        if (StringUtils.hasText(fileProperties.getOss().getBucketDomain())) {
            return fileProperties.getOss().getBucketDomain() + "/" + objectKey;
        }
        return "https://" + bucketName + "." + fileProperties.getOss().getEndpoint().replace("https://", "").replace("http://", "") + "/" + objectKey;
    }

    private boolean isAdmin() {
        String roles = request.getHeader("X-User-Roles");
        if (!StringUtils.hasText(roles)) {
            return false;
        }
        return List.of(roles.split(",")).stream()
                .map(String::trim)
                .anyMatch(role -> "ADMIN".equalsIgnoreCase(role)
                        || "ROLE_ADMIN".equalsIgnoreCase(role)
                        || "SUPER_ADMIN".equalsIgnoreCase(role));
    }
}
