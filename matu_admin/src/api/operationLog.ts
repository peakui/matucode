import { request } from '@/utils/request'
import type {
  OperationLogListParams,
  OperationLogVO,
  PageResponse,
} from '@/api/types'

export const getAdminOperationLogListApi = (params: OperationLogListParams) => {
  return request<PageResponse<OperationLogVO>>({
    url: '/admin/info/operation-logs',
    method: 'get',
    params,
  })
}

export const getAdminOperationLogDetailApi = (id: string) => {
  return request<OperationLogVO>({
    url: `/admin/info/operation-logs/${id}`,
    method: 'get',
  })
}
