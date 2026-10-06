export interface AiAdminStatusVO {
  recordStoreAvailable: boolean
  mcpEnabled: boolean
  mcpServerCount: number
  ragEnabled: boolean
  chatModel?: string | null
  embeddingModel?: string | null
}

export interface AdminAiConversationVO {
  id: number
  conversationId: string
  userId: string
  title?: string | null
  scene?: string | null
  messageCount: number
  createdAt?: string
  updatedAt?: string
}

export interface AdminAiMessageVO {
  id: number
  conversationId: string
  userId: string
  role: string
  content: string
  createdAt?: string
}

export interface McpServerVO {
  name: string
  url: string
  enabled: boolean
  allowTools: string[]
  allowToolCount: number
}

export interface McpToolVO {
  server: string
  name: string
  description?: string
  inputSchema?: unknown
}

export interface AdminAiConversationListParams {
  keyword?: string
  pageNum?: number
  pageSize?: number
}

export interface AdminAiMessageListParams {
  userId?: string
  pageNum?: number
  pageSize?: number
}
