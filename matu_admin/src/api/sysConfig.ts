import { request } from '@/utils/request'
import type {
  CreateSysConfigRequest,
  PageResponse,
  SysConfigListParams,
  SysConfigVO,
  UpdateSysConfigRequest,
} from '@/api/types'

export const getAdminSysConfigListApi = (params: SysConfigListParams) => {
  return request<PageResponse<SysConfigVO>>({
    url: '/admin/info/sys-configs',
    method: 'get',
    params,
  })
}

export const getAdminSysConfigDetailApi = (configKey: string) => {
  return request<SysConfigVO>({
    url: `/admin/info/sys-configs/${encodeURIComponent(configKey)}`,
    method: 'get',
  })
}

export const createAdminSysConfigApi = (data: CreateSysConfigRequest) => {
  return request<SysConfigVO>({
    url: '/admin/info/sys-configs',
    method: 'post',
    data,
  })
}

export const updateAdminSysConfigApi = (configKey: string, data: UpdateSysConfigRequest) => {
  return request<SysConfigVO>({
    url: `/admin/info/sys-configs/${encodeURIComponent(configKey)}`,
    method: 'put',
    data,
  })
}

export const deleteAdminSysConfigApi = (configKey: string) => {
  return request<null>({
    url: `/admin/info/sys-configs/${encodeURIComponent(configKey)}`,
    method: 'delete',
  })
}
