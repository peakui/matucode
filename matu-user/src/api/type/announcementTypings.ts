import type { PageResponse } from './postTypings'

export interface ListAnnouncementsParams {
  type?: number
  keyword?: string
  pageNum?: number
  pageSize?: number
}

export interface AnnouncementListItemVO {
  id?: string | number
  title?: string
  type?: number
  priority?: number
  isPinned?: number
  publishTime?: string
  expireTime?: string
  status?: number
  authorId?: string | number
  clickCount?: number
  createdAt?: string
  updatedAt?: string
}

export interface AnnouncementDetailVO extends AnnouncementListItemVO {
  content?: string
}

export type AnnouncementPageResponse = PageResponse<AnnouncementListItemVO>