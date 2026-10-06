export interface ApiResponse<T> {
  code: number
  message?: string
  msg?: string
  success?: boolean
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

export interface CreateSingleConversationRequest {
  targetUserId: string | number
}

export interface CreateGroupConversationRequest {
  conversationName?: string
  memberIds: Array<string | number>
}

export interface ConversationMemberVO {
  id?: string | number
  conversationId?: string | number
  userId?: string | number
  username?: string
  nickname?: string
  avatarUrl?: string
  userAvatar?: string
  userAvatarUrl?: string
  headImgUrl?: string
  avatar?: string
  role?: number
  isMuted?: number
  isTop?: number
  lastReadMessageId?: string | number
  unreadCount?: number
  joinedAt?: string
}

export interface MessageVO {
  id?: string | number
  conversationId?: string | number
  senderId?: string | number
  messageType?: number
  content?: string
  fileUrl?: string | null
  fileName?: string | null
  fileSize?: number
  replyToId?: string | number | null
  isDeleted?: number
  isRecall?: number
  recallTime?: string | null
  readStatus?: number
  createdAt?: string
}

export interface ConversationVO {
  id?: string | number
  conversationType?: number
  conversationName?: string | null
  creatorId?: string | number
  lastMessageId?: string | number | null
  lastMessageTime?: string | null
  memberCount?: number
  isMuted?: number
  isTop?: number
  lastReadMessageId?: string | number | null
  unreadCount?: number
  lastMessage?: MessageVO | null
  members?: ConversationMemberVO[] | null
  createdAt?: string
  updatedAt?: string
}

export interface SendMessageRequest {
  messageType: number
  content?: string
  fileUrl?: string
  fileName?: string
  fileSize?: number
  replyToId?: string | number
}

export interface MarkConversationReadRequest {
  lastReadMessageId?: string | number
}

export interface NotificationVO {
  id?: string | number
  type?: string
  sourceType?: string
  sourceId?: string | number
  sourceTitle?: string | null
  commentId?: string | number
  fromUserId?: string | number
  fromNickname?: string | null
  fromAvatar?: string | null
  contentPreview?: string | null
  isRead?: number
  createdAt?: string
}

export interface MarkNotificationsReadRequest {
  ids?: Array<string | number>
  all?: boolean
}

export interface CommentReplyNotificationData {
  notificationId?: string | number
  sourceType?: string
  sourceId?: string | number
  sourceTitle?: string | null
  commentId?: string | number
  fromUserId?: string | number
  fromNickname?: string | null
  fromAvatar?: string | null
  contentPreview?: string | null
  createdAt?: string
}

export type PrivateMessageWsEventType = 'MESSAGE_NEW' | 'CONVERSATION_UPDATE' | 'MESSAGE_READ' | 'MESSAGE_RECALL' | 'MESSAGE_DELETE' | 'COMMENT_REPLY'

export interface MessageReadEventData {
  userId?: string | number
  lastReadMessageId?: string | number
}

export interface PrivateMessageWsEvent {
  type: PrivateMessageWsEventType
  conversationId?: string | number
  messageId?: string | number | null
  senderId?: string | number
  receiverUserIds?: Array<string | number>
  data?: MessageVO | ConversationVO | MessageReadEventData | null
  timestamp?: string
}
