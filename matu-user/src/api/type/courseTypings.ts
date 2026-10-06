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

export interface CourseListParams {
  categoryId?: string | number
  level?: number
  freeOnly?: boolean
  keyword?: string
  pageNum?: number
  pageSize?: number
  sortBy?: 'latest' | 'popular' | 'rating'
  status?: number
  // 只看当前用户发布的课程（含草稿），管理员忽略此参数
  mine?: boolean
}

export interface CourseListItemVO {
  id?: string | number
  instructorId?: string | number
  instructorName?: string
  instructorNickname?: string
  instructorAvatarUrl?: string
  categoryId?: string | number
  title?: string
  subtitle?: string
  coverUrl?: string
  price?: number | string
  originalPrice?: number | string
  level?: number
  studentCount?: number
  chapterCount?: number
  videoCount?: number
  totalDuration?: number
  rating?: number | string
  isFree?: number
  status?: number
  createdAt?: string
  updatedAt?: string
  publishedAt?: string
}

export interface CourseChapterVO {
  id?: string | number
  courseId?: string | number
  chapterTitle?: string
  chapterDesc?: string
  sortOrder?: number
  videoCount?: number
  duration?: number
  isFreePreview?: number
  videos?: CourseVideoVO[]
}

export interface CourseVideoVO {
  id?: string | number
  chapterId?: string | number
  videoTitle?: string
  videoDesc?: string
  videoUrl?: string
  coverUrl?: string
  duration?: number
  fileSize?: number
  resolution?: string
  sortOrder?: number
  playCount?: number
  isFreePreview?: number
}

export interface CourseArticleVO {
  id?: string | number
  courseId?: string | number
  chapterId?: string | number
  title?: string
  content?: string
  wordCount?: number
  readTime?: number
  viewCount?: number
  sortOrder?: number
}

export interface CourseDetailVO extends CourseListItemVO {
  description?: string
  originalPrice?: number | string
  language?: string
  ratingCount?: number
  vipRequired?: boolean
  canWatch?: boolean
  status?: number
  chapters?: CourseChapterVO[]
  articles?: CourseArticleVO[]
}

export interface VideoPlayVO {
  courseId?: string | number
  chapterId?: string | number
  videoId?: string | number
  title?: string
  videoUrl?: string
  freePreview?: boolean
  vipRequired?: boolean
  playable?: boolean
  message?: string
}

export interface CreateCourseRequest {
  categoryId?: string | number
  title: string
  subtitle?: string
  description?: string
  coverUrl?: string
  price?: number | string
  originalPrice?: number | string
  level?: number
  language?: string
  isFree?: number
}

export interface UpdateCourseRequest extends Partial<CreateCourseRequest> {
  status?: number
}

export interface CreateChapterRequest {
  courseId: string | number
  chapterTitle: string
  chapterDesc?: string
  sortOrder?: number
  isFreePreview?: number
}

export interface InitCourseVideoUploadRequest {
  chapterId: string | number
  originalName: string
  fileType?: string
  fileSize: number
  fileMd5?: string
  chunkSize: number
  chunkCount: number
  bucketName?: string
}

export interface CourseVideoUploadInitVO {
  fileId?: string | number
  uploadId?: string
  chunkCount?: number
  chunkSize?: number
  uploadedChunks?: number[]
  chunkUploadUrl?: string
  chunkStatusUrl?: string
  completeUrl?: string
}

export interface CompleteCourseVideoUploadRequest {
  chapterId: string | number
  fileId: string | number
  videoTitle: string
  videoDesc?: string
  coverUrl?: string
  duration?: number
  resolution?: string
  sortOrder?: number
  isFreePreview?: number
}

export interface CreateVideoRequest {
  chapterId: string | number
  videoTitle: string
  videoDesc?: string
  videoUrl: string
  coverUrl?: string
  duration?: number
  fileSize?: number
  resolution?: string
  sortOrder?: number
  isFreePreview?: number
}

export interface UpdateVideoRequest {
  chapterId?: string | number
  videoTitle?: string
  videoDesc?: string
  videoUrl?: string
  coverUrl?: string
  duration?: number
  fileSize?: number
  resolution?: string
  sortOrder?: number
  isFreePreview?: number
  status?: number
}

export interface CreateArticleRequest {
  courseId: string | number
  chapterId?: string | number
  title: string
  content: string
  sortOrder?: number
  status?: number
}

export interface UpdateArticleRequest {
  chapterId?: string | number
  title?: string
  content?: string
  sortOrder?: number
  status?: number
}

export interface UpdateProgressRequest {
  videoId: string | number
  progressPercent: number
  watchedDuration?: number
}

export interface CourseProgressVO {
  id?: string | number
  courseId?: string | number
  videoId?: string | number
  progressPercent?: number | string
  watchedDuration?: number
  lastWatchTime?: string
  isCompleted?: number
  completedAt?: string
}

export interface CreateNoteRequest {
  courseId: string | number
  videoId?: string | number
  content: string
  timestamp?: number
  isPublic?: number
}

export interface CourseNoteVO {
  id?: string | number
  userId?: string | number
  courseId?: string | number
  videoId?: string | number
  content?: string
  timestamp?: number
  isPublic?: number
  likeCount?: number
  createdAt?: string
}

export interface CreateReviewRequest {
  courseId: string | number
  rating: number
  content?: string
}

export interface CourseReviewVO {
  id?: string | number
  userId?: string | number
  courseId?: string | number
  rating?: number
  content?: string
  likeCount?: number
  isVerifiedPurchase?: number
  createdAt?: string
}

export interface CourseCertificateVO {
  id?: string | number
  userId?: string | number
  courseId?: string | number
  certificateNo?: string
  certificateUrl?: string
  issueDate?: string
  expireDate?: string
}
