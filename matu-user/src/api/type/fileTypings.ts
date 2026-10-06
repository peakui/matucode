export interface ApiResponse<T> {
  code: number
  message: string
  msg?: string
  success?: boolean
  data: T
}

export interface FileUploadRequest {
  ownerType?: number
  isPublic?: number
  bucketName?: string
  expiresAt?: string
  bizType?: string
  folder?: string
  fileMd5?: string
}

export interface ChunkInitRequest {
  originalName: string
  fileType?: string
  fileSize: number
  fileMd5: string
  chunkSize: number
  chunkCount: number
  bucketName?: string
  ownerType?: number
  isPublic?: number
  expiresAt?: string
}

export interface ChunkInitVO {
  fileId?: string | number
  uploadId?: string | null
  chunkCount?: number
  chunkSize?: number
  uploadedChunks?: number[]
  completed?: boolean
  fileUrl?: string
}

export interface ChunkUploadRequest {
  fileId: string | number
  chunkNo: number
  chunkMd5?: string
  file: Blob | File
}

export interface ChunkStatusVO {
  fileId?: string | number
  uploadId?: string
  chunkCount?: number
  uploadedChunks?: number[]
  completed?: boolean
}

export interface FileInfoVO {
  id?: string | number
  fileName?: string
  originalName?: string
  originalFileName?: string
  filePath?: string
  url?: string
  fileUrl?: string
  fullUrl?: string
  downloadUrl?: string
  contentType?: string
  fileType?: string
  fileSize?: number
  fileMd5?: string
  bucketName?: string
  ownerId?: string | number
  ownerType?: number
  isPublic?: number
  downloadCount?: number
  status?: number
  uploadId?: string | null
  bizType?: string
  folder?: string
  createdAt?: string
  expiresAt?: string | null
}
