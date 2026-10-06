import axios, {
  type AxiosError,
  type AxiosHeaders,
  type AxiosInstance,
  type AxiosRequestConfig,
  type AxiosResponse,
} from 'axios'
import JSONBig from 'json-bigint'
import { ElMessage } from 'element-plus'
import { expireAdminSession } from './auth'

export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

const SUCCESS_CODES: number[] = [0, 200]
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

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL?.trim() || '/api',
  timeout: 10000,
  transformResponse: [(data) => (typeof data === 'string' ? parseResponseData(data) : data)],
})

service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('admin_token')
    const headers = config.headers as AxiosHeaders | undefined

    if (headers) {
      if (token && !config.url?.startsWith('/auth/login')) {
        headers.set('Authorization', `Bearer ${token.replace(/^Bearer\s+/i, '')}`)
      }

      headers.set('Content-Type', 'application/json;charset=UTF-8')
    }

    return config
  },
  (error: AxiosError) => {
    ElMessage.error(error.message || '请求发送失败')
    return Promise.reject(error)
  },
)

service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data

    if (!SUCCESS_CODES.includes(res.code)) {
      ElMessage.error(res.message || '请求失败')

      if (res.code === 401) {
        expireAdminSession()
      }

      return Promise.reject(res)
    }

    return response
  },
  (error: AxiosError<{ message?: string }>) => {
    const status = error.response?.status
    const hint = error.response?.data?.message || ''
    if (status === 401 || (status === 400 && ['未登录', '登录已失效', '请重新登录', '登录凭证无效'].some((text) => hint.includes(text)))) {
      expireAdminSession()
    }
    const message = error.response?.data?.message || error.message || '网络异常'
    ElMessage.error(message)
    return Promise.reject(error)
  },
)

export const request = <T = unknown>(config: AxiosRequestConfig) => {
  return service
    .request<ApiResponse<T>, AxiosResponse<ApiResponse<T>>>(config)
    .then((response) => response.data)
}

export default service
