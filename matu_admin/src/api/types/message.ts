export interface ConversationMemberVO {
  id: string
  conversationId: string
  userId: string
  role: number
  isMuted: number
  isTop: number
  lastReadMessageId?: string | null
  unreadCount: number
  joinedAt?: string
}

export interface AdminConversationVO {
  id: string
  conversationType: number
  conversationName?: string | null
  creatorId?: string | null
  lastMessageId?: string | null
  lastMessageTime?: string | null
  lastMessagePreview?: string | null
  memberCount: number
  isDeleted: number
  createdAt?: string
  updatedAt?: string
  members?: ConversationMemberVO[] | null
}

export interface AdminMessageVO {
  id: string
  conversationId: string
  senderId: string
  messageType: number
  content?: string | null
  fileUrl?: string | null
  fileName?: string | null
  fileSize?: number | null
  replyToId?: string | null
  isDeleted: number
  isRecall: number
  recallTime?: string | null
  readStatus: number
  createdAt?: string
}

export interface AdminConversationListParams {
  keyword?: string
  conversationType?: number
  creatorId?: string
  pageNum?: number
  pageSize?: number
}

export interface AdminMessageListParams {
  conversationId?: string
  senderId?: string
  messageType?: number
  isRecall?: number
  isDeleted?: number
  keyword?: string
  startTime?: string
  endTime?: string
  pageNum?: number
  pageSize?: number
}
