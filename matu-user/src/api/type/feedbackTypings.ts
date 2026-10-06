import type { PageResponse } from './postTypings'

export interface FeedbackAttachmentVO {
  name?: string
  url?: string
  size?: number
}

export type FeedbackExtraInfo = Record<string, string | number | boolean | null | undefined>

export interface CreateFeedbackRequest {
  contactEmail?: string
  type?: number
  title: string
  content: string
  attachments?: FeedbackAttachmentVO[]
  extraInfo?: FeedbackExtraInfo
}

export interface ListMyFeedbacksParams {
  pageNum?: number
  pageSize?: number
}

export interface FeedbackListItemVO {
  id?: string | number
  userId?: string | number
  username?: string
  contactEmail?: string
  type?: number
  title?: string
  status?: number
  priority?: number
  assigneeId?: string | number | null
  repliedAt?: string | null
  resolvedAt?: string | null
  createdAt?: string
  updatedAt?: string
}

export interface FeedbackDetailVO extends FeedbackListItemVO {
  history?: { id: string | number; fromStatus: number; toStatus: number; replyContent?: string; createdAt: string }[]
  content?: string
  attachments?: FeedbackAttachmentVO[]
  extraInfo?: FeedbackExtraInfo
  replyContent?: string | null
  ipAddress?: string
}

export type FeedbackPageResponse = PageResponse<FeedbackListItemVO>
