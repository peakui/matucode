export type FeedbackType = 0 | 1 | 2 | 3 | 4
export type FeedbackStatus = 0 | 1 | 2 | 3 | 4
export type FeedbackPriority = 0 | 1 | 2 | 3

export interface FeedbackAttachment {
  name: string
  url: string
  size: number
}

export interface FeedbackVO {
  history?: { id: string; operatorId: string; fromStatus: FeedbackStatus; toStatus: FeedbackStatus; replyContent?: string; createdAt: string }[]
  id: string
  userId?: string
  username?: string
  contactEmail?: string
  type: FeedbackType
  title: string
  content?: string
  attachments?: FeedbackAttachment[]
  extraInfo?: Record<string, unknown>
  status: FeedbackStatus
  priority: FeedbackPriority
  assigneeId?: string | null
  replyContent?: string | null
  repliedAt?: string | null
  resolvedAt?: string | null
  ipAddress?: string
  createdAt: string
  updatedAt: string
}

export interface FeedbackListParams {
  type?: FeedbackType
  status?: FeedbackStatus
  priority?: FeedbackPriority
  assigneeId?: string
  userId?: string
  keyword?: string
  pageNum?: number
  pageSize?: number
}

export interface UpdateFeedbackRequest {
  status?: FeedbackStatus
  priority?: FeedbackPriority
  assigneeId?: string
  replyContent?: string
}
