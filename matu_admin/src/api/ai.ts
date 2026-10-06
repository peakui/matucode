import { request } from '@/utils/request'
import type {
  AdminAiConversationListParams,
  AdminAiConversationVO,
  AdminAiMessageListParams,
  AdminAiMessageVO,
  AiAdminStatusVO,
  McpServerVO,
  McpToolVO,
  PageResponse,
} from '@/api/types'

export const getAdminAiStatusApi = () => {
  return request<AiAdminStatusVO>({
    url: '/ai/admin/status',
    method: 'get',
  })
}

export const getAdminAiConversationListApi = (params: AdminAiConversationListParams) => {
  return request<PageResponse<AdminAiConversationVO>>({
    url: '/ai/admin/conversations',
    method: 'get',
    params,
  })
}

export const getAdminAiMessageListApi = (conversationId: string, params: AdminAiMessageListParams) => {
  return request<PageResponse<AdminAiMessageVO>>({
    url: `/ai/admin/conversations/${encodeURIComponent(conversationId)}/messages`,
    method: 'get',
    params,
  })
}

export const getAdminAiTaskApi = (taskId: string) => {
  return request<Record<string, unknown>>({
    url: `/ai/admin/tasks/${encodeURIComponent(taskId)}`,
    method: 'get',
  })
}

export const getAdminMcpServerListApi = () => {
  return request<McpServerVO[]>({
    url: '/ai/admin/mcp/servers',
    method: 'get',
  })
}

export const getAdminMcpToolListApi = () => {
  return request<McpToolVO[]>({
    url: '/ai/admin/mcp/tools',
    method: 'get',
  })
}
