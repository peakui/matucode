export interface ApiResponse<T> {
  code: number
  message: string
  msg?: string
  success?: boolean
  data: T
}

export interface PageResponse<T> {
  pageNum: number
  pageSize: number
  total: number
  totalPages: number
  records: T[]
}

export interface PostImageRequest {
  imageUrl: string
  imageType?: string
  sortOrder?: number
}

export interface CreatePostRequest {
  title: string
  summary?: string
  categoryId?: string | number
  contentType: number
  content: string
  status?: number
  images?: PostImageRequest[]
  tagNames?: string[]
}

export interface UpdatePostRequest extends CreatePostRequest {
  changeDesc?: string
}

export interface CreateCommentRequest {
  parentId?: string | number
  replyToUserId?: string | number
  content: string
}

export interface CreateReportRequest {
  commentId?: string | number
  reportType: number
  reportReason: string
}

export interface PostImageVO {
  id?: string | number
  imageUrl?: string
  imageType?: string
  sortOrder?: number
}

export interface CommentVO {
  id?: string | number
  postId?: string | number
  userId?: string | number
  parentId?: string | number
  replyToUserId?: string | number
  content?: string
  likeCount?: number
  liked?: boolean
  replyCount?: number
  status?: number
  ipAddress?: string
  userName?: string
  avatarUrl?: string
  userAvatar?: string
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
  commentAuthorTitle?: string
  userTitle?: string
  titleVerified?: number | boolean
  authorIsVip?: number | boolean
  owner?: boolean
  createdAt?: string
  updatedAt?: string
  replies?: CommentVO[]
}

export interface PostCategoryVO {
  id?: string | number
  parentId?: string | number
  categoryName?: string
  categoryDesc?: string
  iconUrl?: string
  sortOrder?: number
  postCount?: number
  status?: number
  createdAt?: string
}

export interface PostTagVO {
  id?: string | number
  tagName?: string
  tagDesc?: string
  postCount?: number
  status?: number
  createdAt?: string
}

export interface PostListItemVO {
  id?: string | number
  userId?: string | number
  username?: string
  nickname?: string
  authorName?: string
  avatarUrl?: string
  authorAvatar?: string
  userAvatar?: string
  userAvatarUrl?: string
  headImgUrl?: string
  schoolName?: string
  authorSchoolName?: string
  schoolVerified?: number | boolean
  companyName?: string
  authorCompanyName?: string
  companyVerified?: number | boolean
  titleVerified?: number | boolean
  authorTitle?: string
  userTitle?: string
  jobTitle?: string
  authorIsVip?: number | boolean
  owner?: boolean
  categoryId?: string | number
  categoryName?: string
  title?: string
  summary?: string
  viewCount?: number
  likeCount?: number
  commentCount?: number
  collectCount?: number
  shareCount?: number
  status?: number
  isTop?: number
  isEssence?: number
  createdAt?: string
  updatedAt?: string
  publishedAt?: string
  tags?: string[]
  coverImage?: string
}

export interface PostDetailVO {
  id?: string | number
  userId?: string | number
  username?: string
  nickname?: string
  authorName?: string
  signature?: string
  bio?: string
  personalSignature?: string
  profile?: string
  avatarUrl?: string
  authorAvatar?: string
  userAvatar?: string
  userAvatarUrl?: string
  headImgUrl?: string
  schoolName?: string
  authorSchoolName?: string
  schoolVerified?: number | boolean
  companyName?: string
  authorCompanyName?: string
  companyVerified?: number | boolean
  authorTitle?: string
  userTitle?: string
  jobTitle?: string
  titleVerified?: number | boolean
  authorIsVip?: number | boolean
  owner?: boolean
  categoryId?: string | number
  categoryName?: string
  title?: string
  summary?: string
  contentType?: number
  content?: string
  wordCount?: number
  readTime?: number
  version?: number
  viewCount?: number
  likeCount?: number
  commentCount?: number
  collectCount?: number
  shareCount?: number
  status?: number
  isTop?: number
  isEssence?: number
  isLock?: number
  lockReason?: string
  liked?: boolean
  collected?: boolean
  topCommentCount?: number
  createdAt?: string
  updatedAt?: string
  publishedAt?: string
  tags?: string[]
  images?: PostImageVO[]
  recentComments?: CommentVO[]
}

export interface ListPostsParams {
  categoryId?: string | number
  status?: number
  keyword?: string
  userId?: string | number
  pageNum?: number
  pageSize?: number
  sortBy?: string
}

export interface ListDraftsParams {
  pageNum?: number
  pageSize?: number
}

export interface ListTagsParams {
  keyword?: string
}

export interface CollectPostParams {
  collectFolderId?: string | number
}
