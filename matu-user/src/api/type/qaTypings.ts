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

export interface CreateQuestionRequest {
  categoryId?: string | number
  title: string
  content: string
  bountyPoints?: number
}

export interface UpdateQuestionRequest {
  categoryId?: string | number
  title?: string
  content?: string
  bountyPoints?: number
  status?: number
}

export interface CreateAnswerRequest {
  content: string
}

export interface UpdateAnswerRequest {
  content: string
}

export interface CreateQaCommentRequest {
  targetType: number
  targetId: string | number
  parentId?: string | number
  replyToUserId?: string | number
  content: string
}

export interface ListQuestionsParams {
  categoryId?: string | number
  status?: number
  keyword?: string
  userId?: string | number
  pageNum?: number
  pageSize?: number
  sortBy?: string
}

export interface PageParams {
  pageNum?: number
  pageSize?: number
}

export interface QaAnswerVO {
  id?: string | number
  questionId?: string | number
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
  content?: string
  likeCount?: number
  dislikeCount?: number
  isAccepted?: number
  createdAt?: string
}

export interface QaCategoryVO {
  id?: string | number
  parentId?: string | number
  categoryName?: string
  categoryDesc?: string
  iconUrl?: string
  sortOrder?: number
  questionCount?: number
  children?: QaCategoryVO[]
}

export interface QaCommentVO {
  id?: string | number
  targetType?: number
  targetId?: string | number
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
  parentId?: string | number
  replyToUserId?: string | number
  content?: string
  likeCount?: number
  liked?: boolean
  createdAt?: string
  children?: QaCommentVO[]
}

export interface QaQuestionDetailVO {
  id?: string | number
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
  userTitle?: string
  jobTitle?: string
  categoryId?: string | number
  categoryName?: string
  title?: string
  content?: string
  bountyPoints?: number
  viewCount?: number
  answerCount?: number
  followCount?: number
  shareCount?: number
  status?: number
  bestAnswerId?: string | number
  followed?: boolean
  createdAt?: string
  answers?: QaAnswerVO[]
}

export interface QaQuestionListItemVO {
  id?: string | number
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
  userTitle?: string
  jobTitle?: string
  categoryId?: string | number
  categoryName?: string
  title?: string
  content?: string
  bountyPoints?: number
  viewCount?: number
  answerCount?: number
  followCount?: number
  status?: number
  createdAt?: string
}

export interface QaWordCloudVO {
  id?: string | number
  questionId?: string | number
  wordData?: string
  imageUrl?: string
  generatedAt?: string
}
