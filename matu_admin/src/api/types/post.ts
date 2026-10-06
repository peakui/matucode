export interface PageResponse<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  totalPages: number
}

export interface PostCategoryVO {
  id: string
  parentId: string | null
  categoryName: string
  categoryDesc: string
  iconUrl: string
  sortOrder: number
  postCount: number
  status: number
  createdAt: string
}

export interface PostTagVO {
  id: string
  tagName: string
  tagDesc?: string
}

export interface PostListItemVO {
  id: string
  userId: string
  categoryId: string
  title: string
  summary: string
  viewCount: number
  likeCount: number
  commentCount: number
  collectCount: number
  shareCount: number
  status: number
  isTop: number
  isEssence: number
  isLock: number
  lockReason: string
  createdAt: string
  updatedAt: string
  publishedAt: string
}

export interface PostDetailVO extends PostListItemVO {
  content?: string
  contentType?: number
  tags?: PostTagVO[]
  categories?: PostCategoryVO[]
  collaborators?: Array<{
    userId: string
    username?: string
    permission?: number
  }>
}

export interface CommentVO {
  id: string
  postId: string
  userId: string
  parentId: string | null
  content: string
  likeCount: number
  replyCount: number
  status: number
  ipAddress: string
  createdAt: string
  updatedAt: string
  children?: CommentVO[]
  username?: string
}

export interface PostListParams {
  categoryId?: string
  status?: number
  keyword?: string
  userId?: string
  pageNum?: number
  pageSize?: number
  sortBy?: string
}

export interface CreatePostRequest {
  categoryId: string | undefined
  title: string
  summary: string
  content: string
  contentType: number
  status: number
  isTop: number
  isEssence: number
  isLock: number
  lockReason?: string
}

export interface UpdatePostRequest extends CreatePostRequest {}
