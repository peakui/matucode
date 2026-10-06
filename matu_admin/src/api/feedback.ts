import { request } from '@/utils/request'
import type { FeedbackListParams, FeedbackVO, PageResponse, UpdateFeedbackRequest } from '@/api/types'

export const getAdminFeedbackListApi = (params: FeedbackListParams) => {
  return request<PageResponse<FeedbackVO>>({
    url: '/admin/info/feedbacks',
    method: 'get',
    params,
  })
}

export const getAdminFeedbackDetailApi = (id: string) => {
  return request<FeedbackVO>({
    url: `/admin/info/feedbacks/${id}`,
    method: 'get',
  })
}

export const updateAdminFeedbackApi = (id: string, data: UpdateFeedbackRequest) => {
  return request<FeedbackVO>({
    url: `/admin/info/feedbacks/${id}`,
    method: 'put',
    data,
  })
}
