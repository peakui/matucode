export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface PageResponse<T> {
  pageNum: number
  pageSize: number
  total: number
  totalPages: number
  records: T[]
}

export interface CreateCheckRecordRequest {
  title: string
  summary?: string
  content: string
  imageUrls?: string[]
  learnHours?: number
  mood?: number
  location?: string
  status?: number
  checkDate?: string
}

export type UpdateCheckRecordRequest = CreateCheckRecordRequest

export interface CreateCheckCommentRequest {
  parentId?: string | number
  replyToUserId?: string | number
  content: string
}

export interface CreateCheckGroupRequest {
  groupName: string
  groupDesc?: string
  coverImage?: string
  isPublic?: number
}

export interface ListCheckRecordsParams {
  userId?: number
  status?: number
  year?: number
  month?: number
  pageNum?: number
  pageSize?: number
}

export interface CheckRecordVO {
  id?: string | number
  checkId?: string | number
  recordId?: string | number
  userId?: string | number
  username?: string
  nickname?: string
  signature?: string
  bio?: string
  personalSignature?: string
  profile?: string
  avatarUrl?: string
  userAvatar?: string
  userAvatarUrl?: string
  headImgUrl?: string
  schoolName?: string
  authorSchoolName?: string
  userSchoolName?: string
  schoolVerified?: number | boolean
  companyName?: string
  authorCompanyName?: string
  userCompanyName?: string
  companyVerified?: number | boolean
  titleVerified?: number | boolean
  authorTitle?: string
  authorIsVip?: number | boolean
  title?: string
  userTitle?: string
  jobTitle?: string
  owner?: boolean
  liked?: boolean
  recordTitle?: string
  summary?: string
  content?: string
  imageUrls?: string[]
  learnHours?: number
  mood?: number
  location?: string
  ipAddress?: string
  viewCount?: number
  likeCount?: number
  commentCount?: number
  shareCount?: number
  isTop?: number
  isFeatured?: number
  status?: number
  checkDate?: string
  checkTime?: string
  createdAt?: string
  updatedAt?: string
}

export interface CheckRecordListItemVO {
  id?: string | number
  checkId?: string | number
  recordId?: string | number
  userId?: string | number
  username?: string
  nickname?: string
  avatarUrl?: string
  userAvatar?: string
  userAvatarUrl?: string
  headImgUrl?: string
  schoolName?: string
  authorSchoolName?: string
  userSchoolName?: string
  schoolVerified?: number | boolean
  companyName?: string
  authorCompanyName?: string
  userCompanyName?: string
  companyVerified?: number | boolean
  titleVerified?: number | boolean
  authorTitle?: string
  authorIsVip?: number | boolean
  title?: string
  userTitle?: string
  jobTitle?: string
  owner?: boolean
  recordTitle?: string
  summary?: string
  content?: string
  imageUrls?: string[]
  learnHours?: number
  mood?: number
  location?: string
  likeCount?: number
  commentCount?: number
  viewCount?: number
  status?: number
  checkDate?: string
  checkTime?: string
  createdAt?: string
  updatedAt?: string
}

export interface CheckStatisticsVO {
  userId?: string | number
  totalDays?: number
  continuousDays?: number
  maxContinuousDays?: number
  totalArticles?: number
  totalLikesReceived?: number
  totalLearnHours?: number
  lastCheckDate?: string
  checkYear?: number
  checkMonth?: number
}

export interface CheckDaysStatisticsVO {
  userId?: string | number
  totalDays?: number
}

export interface UserAchievementVO {
  id?: string | number
  achievementId?: string | number
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

export interface CheckGroupVO {
  id?: string | number
  groupName?: string
  groupDesc?: string
  coverImage?: string
  creatorId?: string | number
  creatorName?: string
  creatorAvatar?: string
  creatorSchoolName?: string
  creatorSchoolVerified?: number | boolean
  creatorCompanyName?: string
  creatorCompanyVerified?: number | boolean
  creatorTitle?: string
  creatorTitleVerified?: number | boolean
  memberCount?: number
  articleCount?: number
  isPublic?: number
  status?: number
  createdAt?: string
}

export interface CheckCommentVO {
  id?: string | number
  checkId?: string | number
  userId?: string | number
  parentId?: string | number
  replyToUserId?: string | number
  content?: string
  likeCount?: number
  liked?: boolean
  status?: number
  createdAt?: string
  username?: string
  nickname?: string
  avatarUrl?: string
  userAvatar?: string
  userAvatarUrl?: string
  headImgUrl?: string
  schoolName?: string
  authorSchoolName?: string
  commentAuthorSchoolName?: string
  userSchoolName?: string
  schoolVerified?: number | boolean
  companyName?: string
  authorCompanyName?: string
  commentAuthorCompanyName?: string
  userCompanyName?: string
  companyVerified?: number | boolean
  authorTitle?: string
  authorIsVip?: number | boolean
  commentAuthorTitle?: string
  userTitle?: string
  jobTitle?: string
  titleVerified?: number | boolean
  owner?: boolean
  children?: CheckCommentVO[]
}
