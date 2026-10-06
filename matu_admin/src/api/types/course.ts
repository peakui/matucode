export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
  msg?: string
  success?: boolean
}

export interface CourseListParams {
  categoryId?: string | number
  level?: number
  freeOnly?: boolean
  keyword?: string
  pageNum?: number
  pageSize?: number
  sortBy?: 'latest' | 'popular' | 'rating'
}

export interface CourseListItemVO {
  id?: string | number
  instructorId?: string | number
  instructorName?: string
  userId?: string | number
  userName?: string
  username?: string
  nickname?: string
  categoryId?: string | number
  title?: string
  subtitle?: string
  coverUrl?: string
  price?: number | string
  level?: number
  studentCount?: number
  chapterCount?: number
  videoCount?: number
  totalDuration?: number
  rating?: number | string
  isFree?: number
  publishedAt?: string
  status?: number
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
  articles?: CourseArticleVO[]
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
  chapters?: CourseChapterVO[]
  articles?: CourseArticleVO[]
}

export interface VideoPlayVO {
  courseId?: string | number
  chapterId?: string | number
  videoId?: string | number
  title?: string
  videoUrl?: string | null
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

export interface UpdateChapterRequest extends Partial<CreateChapterRequest> {}

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

export interface UpdateVideoRequest extends Partial<Omit<CompleteCourseVideoUploadRequest, 'fileId'>> {
  videoUrl?: string
  fileSize?: number
}

export interface CreateVideoRequest extends Omit<CompleteCourseVideoUploadRequest, 'fileId'> {
  videoUrl: string
  fileSize?: number
}

export interface CreateArticleRequest {
  courseId: string | number
  chapterId?: string | number
  title: string
  content: string
  sortOrder?: number
  status?: number
}

export interface UpdateArticleRequest extends Partial<CreateArticleRequest> {}

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
