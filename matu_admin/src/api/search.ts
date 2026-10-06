import { request } from '@/utils/request'
import type { IndexStatusVO, SyncResultVO } from '@/api/types'

export const getAdminSearchStatusApi = () => {
  return request<IndexStatusVO>({
    url: '/search/admin/status',
    method: 'get',
  })
}

export const reindexAdminSearchApi = () => {
  return request<SyncResultVO>({
    url: '/search/admin/reindex',
    method: 'post',
  })
}
