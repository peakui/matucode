import type { PageResponse } from './post'

export interface FileUploadRequest {
  file: File
  ownerType?: number
  isPublic?: number
  bucketName?: string
  expiresAt?: string
}

export interface FileChunkInitRequest {
  chapterId?: string | number
  originalName: string
  fileType?: string
  fileSize: number
  fileMd5?: string
  chunkSize: number
  chunkCount: number
  bucketName?: string
}

export interface FileChunkInitVO {
  fileId?: string | number
  uploadId?: string
  chunkCount?: number
  chunkSize?: number
  uploadedChunks?: number[]
  chunkUploadUrl?: string
  chunkStatusUrl?: string
  completeUrl?: string
}

export interface FileChunkUploadParams {
  fileId: string | number
  chunkNo: number
  chunkMd5?: string
  file: Blob
}

export interface FileChunkStatusVO {
  fileId?: string | number
  uploadId?: string
  chunkCount?: number
  chunkSize?: number
  uploadedChunks?: number[]
  completed?: boolean
  progress?: number
}

export interface FileChunkCompleteRequest {
  fileId: string | number
  uploadId?: string
}

export interface FileInfoVO {
  fileId?: string | number
  id?: string | number
  fileName?: string
  originalName?: string
  filePath?: string
  fileUrl?: string
  fileType?: string
  fileSize?: number
  fileMd5?: string
  bucketName?: string
  ownerId?: number
  ownerType?: number
  isPublic?: number
  downloadCount?: number
  status?: number
  uploadId?: string
  expiresAt?: string
  createdAt?: string
  updatedAt?: string
}

export interface FileSignedUrlVO {
  fileId?: string | number
  signedUrl?: string
  expireSeconds?: number
}

export interface FileListParams {
  ownerId?: string | number
  ownerType?: number
  fileType?: string
  bucketName?: string
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface FileBucketVO {
  id?: string | number
  bucketName: string
  bucketType?: number
  storageProvider?: string
  endpoint?: string
  region?: string
  maxSize?: number
  usedSize?: number
  fileCount?: number
  status?: number
}

export type FileListResponse = PageResponse<FileInfoVO>
export type FileDetailResponse = FileInfoVO
