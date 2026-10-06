import { request } from '@/utils/request'
import type {
  CreatePlatformMetricRequest,
  PageResponse,
  PlatformMetricListParams,
  PlatformMetricVO,
  UpdatePlatformMetricRequest,
} from '@/api/types'

export const getAdminPlatformMetricListApi = (params: PlatformMetricListParams) => {
  return request<PageResponse<PlatformMetricVO>>({
    url: '/admin/info/platform-metrics',
    method: 'get',
    params,
  })
}

export const getAdminPlatformMetricLatestApi = () => {
  return request<PlatformMetricVO[]>({
    url: '/admin/info/platform-metrics/latest',
    method: 'get',
  })
}

export const createAdminPlatformMetricApi = (data: CreatePlatformMetricRequest) => {
  return request<PlatformMetricVO>({
    url: '/admin/info/platform-metrics',
    method: 'post',
    data,
  })
}

export const updateAdminPlatformMetricApi = (id: string, data: UpdatePlatformMetricRequest) => {
  return request<PlatformMetricVO>({
    url: `/admin/info/platform-metrics/${id}`,
    method: 'put',
    data,
  })
}

export const deleteAdminPlatformMetricApi = (id: string) => {
  return request<null>({
    url: `/admin/info/platform-metrics/${id}`,
    method: 'delete',
  })
}
