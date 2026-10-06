import axios, { type AxiosError, type AxiosHeaders, type AxiosInstance, type AxiosResponse } from 'axios'
import JSONBig from 'json-bigint'
import { ElMessage } from 'element-plus'

import type {
  FileBucketVO,
  FileChunkCompleteRequest,
  FileChunkInitRequest,
  FileChunkInitVO,
  FileChunkStatusVO,
  FileChunkUploadParams,
  FileDetailResponse,
  FileInfoVO,
  FileListParams,
  FileListResponse,
  FileSignedUrlVO,
  FileUploadRequest,
} from './types/file'

interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
  msg?: string
  success?: boolean
}

const SUCCESS_CODES = [0, 200, 20000]
const jsonParser = JSONBig({ storeAsString: true })

const parseResponseData = (raw: string) => {
  if (!raw.trim()) {
    return raw
  }

  try {
    return jsonParser.parse(raw)
  } catch {
    return raw
  }
}

const uploadService: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
  transformResponse: [(data) => (typeof data === 'string' ? parseResponseData(data) : data)],
})

uploadService.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('admin_token')
    const headers = config.headers as AxiosHeaders | undefined

    if (headers) {
      if (token) {
        headers.set('Authorization', `Bearer ${token}`)
      }

      if (!(config.data instanceof FormData)) {
        headers.set('Content-Type', 'application/json;charset=UTF-8')
      } else if (headers.has('Content-Type')) {
        headers.delete('Content-Type')
      }
    }

    return config
  },
  (error: AxiosError) => {
    ElMessage.error(error.message || '请求发送失败')
    return Promise.reject(error)
  },
)

uploadService.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data

    if (!SUCCESS_CODES.includes(res.code) && res.success !== true) {
      ElMessage.error(res.message || res.msg || '请求失败')

      if (res.code === 401) {
        localStorage.removeItem('admin_token')
        localStorage.removeItem('admin_authorization')
        localStorage.removeItem('admin_user_info')
        localStorage.removeItem('admin_logged_in')
        window.location.href = '/login'
      }

      return Promise.reject(res)
    }

    return response
  },
  (error: AxiosError<{ message?: string }>) => {
    const message = error.response?.data?.message || error.message || '网络异常'
    ElMessage.error(message)
    return Promise.reject(error)
  },
)

const unwrap = <T>(response: AxiosResponse<ApiResponse<T>>) => response.data.data

const buildFormData = (data: FileUploadRequest) => {
  const formData = new FormData()
  formData.append('file', data.file)

  if (data.ownerType !== undefined) {
    formData.append('ownerType', String(data.ownerType))
  }

  if (data.isPublic !== undefined) {
    formData.append('isPublic', String(data.isPublic))
  }

  if (data.bucketName) {
    formData.append('bucketName', data.bucketName)
  }

  if (data.expiresAt) {
    formData.append('expiresAt', data.expiresAt)
  }

  return formData
}

export const uploadFileApi = async (data: FileUploadRequest): Promise<FileInfoVO> => {
  const response = await uploadService.post<ApiResponse<FileInfoVO>>('/files/upload', buildFormData(data))
  return response.data.data
}

export const initFileChunkUploadApi = async (data: FileChunkInitRequest): Promise<FileChunkInitVO> => {
  const response = await uploadService.post<ApiResponse<FileChunkInitVO>>('/files/chunk/init', data)
  return unwrap(response)
}

export const uploadFileChunkApi = async (data: FileChunkUploadParams): Promise<unknown> => {
  const formData = new FormData()
  formData.append('file', data.file)

  const response = await uploadService.post<ApiResponse<unknown>>('/files/chunk/upload', formData, {
    params: {
      fileId: data.fileId,
      chunkNo: data.chunkNo,
      ...(data.chunkMd5 ? { chunkMd5: data.chunkMd5 } : {}),
    },
  })

  return response.data.data
}

export const getFileChunkStatusApi = async (fileId: string | number): Promise<FileChunkStatusVO> => {
  const response = await uploadService.get<ApiResponse<FileChunkStatusVO>>('/files/chunk/status', {
    params: { fileId },
  })
  return unwrap(response)
}

export const completeFileChunkUploadApi = async (
  data: FileChunkCompleteRequest,
): Promise<FileInfoVO> => {
  const response = await uploadService.post<ApiResponse<FileInfoVO>>('/files/chunk/complete', data)
  return unwrap(response)
}

export const getFileDetailApi = async (fileId: string | number): Promise<FileDetailResponse> => {
  const response = await uploadService.get<ApiResponse<FileDetailResponse>>(`/files/${String(fileId)}`)
  return unwrap(response)
}

export const getFileDownloadUrlApi = async (fileId: string | number): Promise<FileSignedUrlVO> => {
  const response = await uploadService.get<ApiResponse<FileSignedUrlVO>>(
    `/files/${String(fileId)}/download-url`,
  )
  return unwrap(response)
}

export const listFilesApi = async (params: FileListParams = {}): Promise<FileListResponse> => {
  const response = await uploadService.get<ApiResponse<FileListResponse>>('/files', { params })
  return unwrap(response)
}

export const listFileBucketsApi = async (): Promise<FileBucketVO[]> => {
  const response = await uploadService.get<ApiResponse<FileBucketVO[]>>('/files/buckets')
  return unwrap(response)
}

export const deleteFileApi = async (fileId: string | number): Promise<void> => {
  await uploadService.delete<ApiResponse<null>>(`/files/${String(fileId)}`)
}
