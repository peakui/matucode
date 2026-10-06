export interface CheckRecordListParams {
  userId?: string
  status?: number
  year?: number
  month?: number
  pageNum?: number
  pageSize?: number
}

export interface CheckRecordListItemVO {
  id: string
  userId: string
  title: string
  summary?: string
  learnHours: string
  mood?: number
  location?: string
  viewCount: number
  likeCount: number
  commentCount: number
  isTop?: number
  isFeatured?: number
  status: number
  checkDate: string
  checkTime?: string
  createdAt?: string
  updatedAt?: string
}

export interface CheckRecordVO extends CheckRecordListItemVO {
  content: string
  imageUrls?: string[]
  ipAddress?: string
}

export interface CheckStatisticsVO {
  userId: string
  totalDays: number
  continuousDays: number
  maxContinuousDays: number
  totalArticles: number
  totalLikesReceived: number
  totalLearnHours: string
  lastCheckDate?: string
  checkYear: number
  checkMonth: number
  updatedAt?: string
}

export interface UserAchievementVO {
  id: string
  achievementId: string
  userId: string
  achievementCode?: string
  achievementName?: string
  achievementDesc?: string
  achievementIcon?: string
  achievementType?: number
  conditionType?: number
  conditionValue?: number
  pointReward?: number
  progress?: number
  isClaimed?: number
  achievedAt?: string
}

export interface CheckCommentVO {
  id: string
  checkId: string
  userId: string
  parentId?: string | null
  replyToUserId?: string | null
  content: string
  likeCount: number
  status: number
  createdAt: string
  children?: CheckCommentVO[]
}

export interface CheckGroupVO {
  id: string
  groupName: string
  groupDesc?: string
  coverImage?: string
  creatorId: string
  memberCount: number
  articleCount: number
  isPublic: number
  status: number
  createdAt: string
}

export interface CreateCheckRecordRequest {
  title: string
  summary?: string
  content: string
  imageUrls?: string[]
  learnHours: string
  mood?: number
  location?: string
  checkDate: string
  status?: number
}

export interface UpdateCheckRecordRequest extends Partial<CreateCheckRecordRequest> {}

export interface ClaimAchievementRewardRequest {
  achievementId: string | undefined
}

export interface CreateCheckCommentRequest {
  parentId?: string | null
  replyToUserId?: string | null
  content: string
}

export interface CreateCheckGroupRequest {
  groupName: string
  groupDesc?: string
  coverImage?: string
  isPublic?: number
}

export interface OperateGroupMemberRequest {
  memberUserId: string | undefined
}
