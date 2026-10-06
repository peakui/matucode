export type AnnouncementType = 0 | 1 | 2 | 3
export type AnnouncementStatus = 0 | 1 | 2

export interface AnnouncementVO {
  id: string
  title: string
  content?: string
  type: AnnouncementType
  priority: number
  isPinned: number
  publishTime?: string
  expireTime?: string
  status: AnnouncementStatus
  authorId?: string
  clickCount: number
  createdAt: string
  updatedAt: string
}

export interface AnnouncementListParams {
  type?: AnnouncementType
  status?: AnnouncementStatus
  keyword?: string
  pageNum?: number
  pageSize?: number
}

export interface CreateAnnouncementRequest {
  title: string
  content: string
  type: AnnouncementType
  priority: number
  isPinned: number
  publishTime?: string
  expireTime?: string
  status: AnnouncementStatus
}

export interface UpdateAnnouncementRequest extends CreateAnnouncementRequest {}
