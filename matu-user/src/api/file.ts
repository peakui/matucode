import type { AxiosResponse } from 'axios'
import SparkMD5 from 'spark-md5'
import { request } from './request'
import type { ApiResponse, ChunkInitRequest, ChunkInitVO, ChunkStatusVO, ChunkUploadRequest, FileInfoVO, FileUploadRequest } from './type/fileTypings'

const CHUNK_SIZE = 1 * 1024 * 1024
const CHUNK_THRESHOLD = 1 * 1024 * 1024
const CHUNK_RETRY_LIMIT = 3
const HASH_BLOCK_SIZE = 2 * 1024 * 1024
// Uploads must not inherit the shared 10s timeout: on slow links (campus
// network) a upload legitimately takes minutes, and a mid-flight abort looks
// like a server failure. Give every upload request its own generous budget.
const UPLOAD_TIMEOUT = Number(import.meta.env.VITE_UPLOAD_TIMEOUT || 10 * 60 * 1000)

async function postFile(file: File, data: FileUploadRequest = {}): Promise<FileInfoVO> {
  const formData = new FormData()
  formData.append('file', file)

  Object.entries(data).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim() !== '') {
      formData.append(key, String(value))
    }
  })

  const response: AxiosResponse<ApiResponse<FileInfoVO>> = await request.post('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: UPLOAD_TIMEOUT,
  })
  return response.data.data
}

async function uploadChunk(data: ChunkUploadRequest): Promise<unknown> {
  const formData = new FormData()
  formData.append('file', data.file)

  const response: AxiosResponse<ApiResponse<unknown>> = await request.post('/files/chunk/upload', formData, {
    params: {
      fileId: data.fileId,
      chunkNo: data.chunkNo,
      ...(data.chunkMd5 ? { chunkMd5: data.chunkMd5 } : {}),
    },
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: UPLOAD_TIMEOUT,
  })
  return response.data.data
}

async function initChunkUpload(data: ChunkInitRequest): Promise<ChunkInitVO> {
  const response: AxiosResponse<ApiResponse<ChunkInitVO>> = await request.post('/files/chunk/init', data, { timeout: UPLOAD_TIMEOUT })
  return response.data.data
}

async function completeChunkUpload(fileId: string | number): Promise<FileInfoVO> {
  const response: AxiosResponse<ApiResponse<FileInfoVO>> = await request.post('/files/chunk/complete', { fileId }, { timeout: UPLOAD_TIMEOUT })
  return response.data.data
}

async function getChunkStatus(fileId: string | number): Promise<ChunkStatusVO> {
  const response: AxiosResponse<ApiResponse<ChunkStatusVO | null>> = await request.get('/files/chunk/status', { params: { fileId } })
  return response.data.data || { fileId, uploadedChunks: [] }
}

async function calculateFileMd5(file: File): Promise<string> {
  const spark = new SparkMD5.ArrayBuffer()
  for (let offset = 0; offset < file.size; offset += HASH_BLOCK_SIZE) {
    const block = await file.slice(offset, Math.min(file.size, offset + HASH_BLOCK_SIZE)).arrayBuffer()
    spark.append(block)
  }
  return spark.end()
}

async function compressImage(file: File): Promise<File> {
  if (!file.type.startsWith('image/') || file.type === 'image/gif' || file.size < 512 * 1024) return file
  const imageUrl = URL.createObjectURL(file)
  try {
    const image = new Image()
    image.src = imageUrl
    await image.decode()
    const maxEdge = 2560
    const scale = Math.min(1, maxEdge / Math.max(image.naturalWidth, image.naturalHeight))
    const width = Math.max(1, Math.round(image.naturalWidth * scale))
    const height = Math.max(1, Math.round(image.naturalHeight * scale))
    const canvas = document.createElement('canvas')
    canvas.width = width
    canvas.height = height
    const context = canvas.getContext('2d')
    if (!context) return file
    context.drawImage(image, 0, 0, width, height)
    const outputType = file.type === 'image/png' ? 'image/png' : 'image/jpeg'
    const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, outputType, 0.84))
    if (!blob || blob.size >= file.size) return file
    const extension = outputType === 'image/png' ? '.png' : '.jpg'
    const name = file.name.replace(/\.[^.]+$/, '') + extension
    return new File([blob], name, { type: outputType, lastModified: file.lastModified })
  } catch {
    return file
  } finally {
    URL.revokeObjectURL(imageUrl)
  }
}

export interface ImageUploadOptions extends FileUploadRequest {
  onProgress?: (percent: number, status: string) => void
}

async function uploadWithChunking(file: File, { onProgress, ...metadata }: ImageUploadOptions = {}): Promise<FileInfoVO> {
  const fileMd5 = metadata.fileMd5 || await calculateFileMd5(file)

  if (file.size <= CHUNK_THRESHOLD) {
    onProgress?.(10, '正在上传...')
    return postFile(file, { ...metadata, fileMd5 })
  }

  const chunkCount = Math.ceil(file.size / CHUNK_SIZE)
  onProgress?.(5, '正在检查上传状态...')
  const init = await initChunkUpload({
    originalName: file.name,
    fileType: file.type,
    fileSize: file.size,
    fileMd5,
    chunkSize: CHUNK_SIZE,
    chunkCount,
    ownerType: metadata.ownerType,
    isPublic: metadata.isPublic,
    bucketName: metadata.bucketName,
    expiresAt: metadata.expiresAt,
  })
  if (init.completed && init.fileUrl) return { id: init.fileId, fileUrl: init.fileUrl, url: init.fileUrl, fileMd5 }
  if (init.fileId == null) throw new Error('分片初始化未返回文件 ID')

  const status = await getChunkStatus(init.fileId)
  const uploaded = new Set(status.uploadedChunks || init.uploadedChunks || [])
  const completedAtStart = uploaded.size
  onProgress?.(10 + Math.floor((completedAtStart / chunkCount) * 80), `已续传 ${completedAtStart}/${chunkCount} 个分片`)

  for (let chunkNo = 1; chunkNo <= chunkCount; chunkNo += 1) {
    if (uploaded.has(chunkNo)) continue
    const chunk = file.slice((chunkNo - 1) * CHUNK_SIZE, Math.min(file.size, chunkNo * CHUNK_SIZE))
    let failure: unknown
    for (let attempt = 0; attempt < CHUNK_RETRY_LIMIT; attempt += 1) {
      try {
        await uploadChunk({ fileId: init.fileId, chunkNo, file: chunk })
        failure = undefined
        break
      } catch (error) {
        failure = error
        if (attempt < CHUNK_RETRY_LIMIT - 1) await new Promise((resolve) => window.setTimeout(resolve, 500 * (attempt + 1)))
      }
    }
    if (failure) throw failure
    uploaded.add(chunkNo)
    onProgress?.(10 + Math.floor((uploaded.size / chunkCount) * 80), `已上传 ${uploaded.size}/${chunkCount} 个分片`)
  }

  onProgress?.(95, '正在合并分片...')
  const result = await completeChunkUpload(init.fileId)
  onProgress?.(100, '上传完成')
  return result
}

async function uploadFile(file: File, data: FileUploadRequest = {}): Promise<FileInfoVO> {
  return uploadWithChunking(file, data)
}

async function uploadImage(file: File, { onProgress, ...metadata }: ImageUploadOptions = {}): Promise<FileInfoVO> {
  const optimizedFile = await compressImage(file)
  return uploadWithChunking(optimizedFile, { ...metadata, onProgress })
}

export { calculateFileMd5, completeChunkUpload, getChunkStatus, initChunkUpload, uploadChunk, uploadFile, uploadImage }
