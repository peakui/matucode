export interface ApiResponse<T> {
  code: number
  message?: string
  data: T
}

export interface PageResponse<T> {
  pageNum: number
  pageSize: number
  total: number
  totalPages?: number
  records: T[]
}

export interface PageParams {
  pageNum?: number
  pageSize?: number
}

export interface AiConversationQuery extends PageParams {
  keyword?: string
}

export interface AiConversationVO {
  id: number
  conversationId: string
  title?: string
  scene?: string
  messageCount?: number
  createdAt?: string
  updatedAt?: string
}

export interface AiMessageVO {
  id: number
  role: string
  content: string
  createdAt?: string
}
