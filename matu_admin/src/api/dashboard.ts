import { request } from '@/utils/request'
import type { DashboardOverviewVO } from '@/api/types'

export const getDashboardOverviewApi = () => {
  return request<DashboardOverviewVO>({
    url: '/dashboard/overview',
    method: 'get',
  })
}
