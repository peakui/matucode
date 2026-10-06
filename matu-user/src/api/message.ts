import type { AxiosResponse } from 'axios'
import { request } from './request'
import type {
  ApiResponse,
  ConversationVO,
  CreateGroupConversationRequest,
  CreateSingleConversationRequest,
  MarkConversationReadRequest,
  MarkNotificationsReadRequest,
  MessageVO,
  NotificationVO,
  PageParams,
  PageResponse,
  SendMessageRequest,
} from './type/messageTypings'

const emptyPage = <T>(params: PageParams = {}): PageResponse<T> => ({
  pageNum: Number(params.pageNum || 1),
  pageSize: Number(params.pageSize || 10),
  total: 0,
  totalPages: 0,
  records: [],
})

async function createSingleConversation(targetUserId: string | number): Promise<ConversationVO> {
  const data: CreateSingleConversationRequest = { targetUserId }
  const response: AxiosResponse<ApiResponse<ConversationVO>> = await request.post('/messages/conversations/single', data)
  return response.data.data
}

async function createGroupConversation(data: CreateGroupConversationRequest): Promise<ConversationVO> {
  const response: AxiosResponse<ApiResponse<ConversationVO>> = await request.post('/messages/conversations/group', data)
  return response.data.data
}

async function listConversations(params: PageParams = {}): Promise<PageResponse<ConversationVO>> {
  const response: AxiosResponse<ApiResponse<PageResponse<ConversationVO> | null>> = await request.get('/messages/conversations', { params })
  return response.data.data || emptyPage<ConversationVO>(params)
}

async function getConversationDetail(conversationId: string | number): Promise<ConversationVO> {
  const response: AxiosResponse<ApiResponse<ConversationVO>> = await request.get(`/messages/conversations/${String(conversationId)}`)
  return response.data.data
}

async function topConversation(conversationId: string | number): Promise<void> {
  await request.post(`/messages/conversations/${String(conversationId)}/top`)
}

async function untopConversation(conversationId: string | number): Promise<void> {
  await request.delete(`/messages/conversations/${String(conversationId)}/top`)
}

async function muteConversation(conversationId: string | number): Promise<void> {
  await request.post(`/messages/conversations/${String(conversationId)}/mute`)
}

async function unmuteConversation(conversationId: string | number): Promise<void> {
  await request.delete(`/messages/conversations/${String(conversationId)}/mute`)
}

async function listMessages(conversationId: string | number, params: PageParams = {}): Promise<PageResponse<MessageVO>> {
  const response: AxiosResponse<ApiResponse<PageResponse<MessageVO> | null>> = await request.get(`/messages/conversations/${String(conversationId)}/messages`, { params })
  return response.data.data || emptyPage<MessageVO>({ pageNum: params.pageNum || 1, pageSize: params.pageSize || 20 })
}

async function sendMessage(conversationId: string | number, data: SendMessageRequest): Promise<MessageVO> {
  const response: AxiosResponse<ApiResponse<MessageVO>> = await request.post(`/messages/conversations/${String(conversationId)}/messages`, data)
  return response.data.data
}

async function sendTextMessage(conversationId: string | number, content: string): Promise<MessageVO> {
  return sendMessage(conversationId, { messageType: 1, content })
}

async function markConversationRead(conversationId: string | number, lastReadMessageId?: string | number): Promise<void> {
  const data: MarkConversationReadRequest = lastReadMessageId != null ? { lastReadMessageId } : {}
  await request.post(`/messages/conversations/${String(conversationId)}/read`, data)
}

async function recallMessage(messageId: string | number): Promise<void> {
  await request.post(`/messages/${String(messageId)}/recall`)
}

async function deleteMessage(messageId: string | number): Promise<void> {
  await request.delete(`/messages/${String(messageId)}`)
}

async function listNotifications(params: PageParams = {}): Promise<PageResponse<NotificationVO>> {
  const response: AxiosResponse<ApiResponse<PageResponse<NotificationVO> | null>> = await request.get('/messages/notifications', { params })
  return response.data.data || emptyPage<NotificationVO>({ pageNum: params.pageNum || 1, pageSize: params.pageSize || 20 })
}

async function getNotificationUnreadCount(): Promise<number> {
  const response: AxiosResponse<ApiResponse<number>> = await request.get('/messages/notifications/unread-count')
  return Number(response.data.data || 0)
}

async function markNotificationsRead(data: MarkNotificationsReadRequest): Promise<void> {
  await request.post('/messages/notifications/read', data)
}

export {
  createGroupConversation,
  createSingleConversation,
  deleteMessage,
  getConversationDetail,
  getNotificationUnreadCount,
  listConversations,
  listMessages,
  listNotifications,
  markConversationRead,
  markNotificationsRead,
  muteConversation,
  recallMessage,
  sendMessage,
  sendTextMessage,
  topConversation,
  unmuteConversation,
  untopConversation,
}
