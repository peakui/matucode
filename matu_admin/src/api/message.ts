import { request } from '@/utils/request'
import type {
  AdminConversationListParams,
  AdminConversationVO,
  AdminMessageListParams,
  AdminMessageVO,
  PageResponse,
} from '@/api/types'

export const getAdminConversationListApi = (params: AdminConversationListParams) => {
  return request<PageResponse<AdminConversationVO>>({
    url: '/messages/admin/conversations',
    method: 'get',
    params,
  })
}

export const getAdminConversationDetailApi = (conversationId: string) => {
  return request<AdminConversationVO>({
    url: `/messages/admin/conversations/${conversationId}`,
    method: 'get',
  })
}

export const getAdminMessageListApi = (params: AdminMessageListParams) => {
  return request<PageResponse<AdminMessageVO>>({
    url: '/messages/admin/messages',
    method: 'get',
    params,
  })
}

export const recallAdminMessageApi = (messageId: string) => {
  return request<null>({
    url: `/messages/admin/messages/${messageId}/recall`,
    method: 'post',
  })
}

export const deleteAdminMessageApi = (messageId: string) => {
  return request<null>({
    url: `/messages/admin/messages/${messageId}`,
    method: 'delete',
  })
}
