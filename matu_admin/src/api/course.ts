import { request } from '@/utils/request'
import type {
  ApiResponse,
  CompleteCourseVideoUploadRequest,
  CourseArticleVO,
  CourseCertificateVO,
  CourseChapterVO,
  CourseDetailVO,
  CourseListItemVO,
  CourseListParams,
  CourseNoteVO,
  CourseProgressVO,
  CourseReviewVO,
  CourseVideoUploadInitVO,
  CourseVideoVO,
  CreateArticleRequest,
  CreateChapterRequest,
  CreateCourseRequest,
  CreateNoteRequest,
  CreateReviewRequest,
  CreateVideoRequest,
  InitCourseVideoUploadRequest,
  UpdateArticleRequest,
  UpdateChapterRequest,
  UpdateCourseRequest,
  UpdateProgressRequest,
  UpdateVideoRequest,
  VideoPlayVO,
} from './types/course'
import type { PageResponse } from './types/post'

const isSuccessResponse = (response: ApiResponse<unknown>) =>
  response.code === 0 || response.code === 200 || response.code === 20000 || response.success === true

const getErrorMessage = (response: ApiResponse<unknown>, fallback: string) =>
  response.message || response.msg || fallback

async function listCourses(params: CourseListParams = {}): Promise<PageResponse<CourseListItemVO>> {
  const response = await request<PageResponse<CourseListItemVO> | null>({
    url: '/courses',
    method: 'get',
    params,
  })

  if (!isSuccessResponse(response)) {
    throw new Error(getErrorMessage(response, '课程列表加载失败'))
  }

  return response.data || { pageNum: 1, pageSize: 10, total: 0, totalPages: 0, records: [] }
}

async function getCourseDetail(courseId: string | number): Promise<CourseDetailVO> {
  const response = await request<CourseDetailVO>({
    url: `/courses/${String(courseId)}`,
    method: 'get',
  })
  return response.data
}

async function getVideoPlayInfo(videoId: string | number): Promise<VideoPlayVO> {
  const response = await request<VideoPlayVO>({
    url: `/courses/videos/${String(videoId)}/play`,
    method: 'get',
  })
  return response.data
}

async function createCourse(data: CreateCourseRequest): Promise<CourseDetailVO> {
  const response = await request<CourseDetailVO>({
    url: '/courses',
    method: 'post',
    data,
  })
  return response.data
}

async function updateCourse(
  courseId: string | number,
  data: UpdateCourseRequest,
): Promise<CourseDetailVO> {
  const response = await request<CourseDetailVO>({
    url: `/courses/${String(courseId)}`,
    method: 'put',
    data,
  })
  return response.data
}

async function publishCourse(courseId: string | number): Promise<void> {
  await request({
    url: `/courses/${String(courseId)}/publish`,
    method: 'post',
  })
}

async function offlineCourse(courseId: string | number): Promise<void> {
  await request({
    url: `/courses/${String(courseId)}/offline`,
    method: 'post',
  })
}

async function createChapter(data: CreateChapterRequest): Promise<CourseChapterVO> {
  const response = await request<CourseChapterVO>({
    url: '/courses/chapters',
    method: 'post',
    data,
  })
  return response.data
}

async function updateChapter(
  chapterId: string | number,
  data: UpdateChapterRequest,
): Promise<CourseChapterVO> {
  const response = await request<CourseChapterVO>({
    url: `/courses/chapters/${String(chapterId)}`,
    method: 'put',
    data,
  })
  return response.data
}

async function deleteChapter(chapterId: string | number): Promise<void> {
  await request({
    url: `/courses/chapters/${String(chapterId)}`,
    method: 'delete',
  })
}

async function initCourseVideoUpload(
  data: InitCourseVideoUploadRequest,
): Promise<CourseVideoUploadInitVO> {
  const response = await request<CourseVideoUploadInitVO>({
    url: '/courses/videos/upload/init',
    method: 'post',
    data,
  })
  return response.data
}

async function completeCourseVideoUpload(
  data: CompleteCourseVideoUploadRequest,
): Promise<CourseVideoVO> {
  const response = await request<CourseVideoVO>({
    url: '/courses/videos/upload/complete',
    method: 'post',
    data,
  })
  return response.data
}

async function createVideo(data: CreateVideoRequest): Promise<CourseVideoVO> {
  const response = await request<CourseVideoVO>({
    url: '/courses/videos',
    method: 'post',
    data,
  })
  return response.data
}

async function updateVideo(
  videoId: string | number,
  data: UpdateVideoRequest,
): Promise<CourseVideoVO> {
  const response = await request<CourseVideoVO>({
    url: `/courses/videos/${String(videoId)}`,
    method: 'put',
    data,
  })
  return response.data
}

async function deleteVideo(videoId: string | number): Promise<void> {
  await request({
    url: `/courses/videos/${String(videoId)}`,
    method: 'delete',
  })
}

async function createArticle(data: CreateArticleRequest): Promise<CourseArticleVO> {
  const response = await request<CourseArticleVO>({
    url: '/courses/articles',
    method: 'post',
    data,
  })
  return response.data
}

async function updateArticle(
  articleId: string | number,
  data: UpdateArticleRequest,
): Promise<CourseArticleVO> {
  const response = await request<CourseArticleVO>({
    url: `/courses/articles/${String(articleId)}`,
    method: 'put',
    data,
  })
  return response.data
}

async function deleteArticle(articleId: string | number): Promise<void> {
  await request({
    url: `/courses/articles/${String(articleId)}`,
    method: 'delete',
  })
}

async function updateProgress(
  courseId: string | number,
  data: UpdateProgressRequest,
): Promise<CourseProgressVO> {
  const response = await request<CourseProgressVO>({
    url: `/courses/${String(courseId)}/progress`,
    method: 'post',
    data,
  })
  return response.data
}

async function getMyProgress(courseId: string | number): Promise<CourseProgressVO[]> {
  const response = await request<CourseProgressVO[]>({
    url: `/courses/${String(courseId)}/progress/mine`,
    method: 'get',
  })
  return response.data || []
}

async function createNote(data: CreateNoteRequest): Promise<CourseNoteVO> {
  const response = await request<CourseNoteVO>({
    url: '/courses/notes',
    method: 'post',
    data,
  })
  return response.data
}

async function listNotes(params: {
  courseId: string | number
  videoId?: string | number
  onlyPublic?: boolean
  pageNum?: number
  pageSize?: number
}): Promise<PageResponse<CourseNoteVO>> {
  const response = await request<PageResponse<CourseNoteVO> | null>({
    url: '/courses/notes',
    method: 'get',
    params,
  })

  return (
    response.data || {
      pageNum: Number(params.pageNum || 1),
      pageSize: Number(params.pageSize || 10),
      total: 0,
      totalPages: 0,
      records: [],
    }
  )
}

async function createReview(data: CreateReviewRequest): Promise<CourseReviewVO> {
  const response = await request<CourseReviewVO>({
    url: '/courses/reviews',
    method: 'post',
    data,
  })
  return response.data
}

async function listReviews(
  courseId: string | number,
  params: { pageNum?: number; pageSize?: number } = {},
): Promise<PageResponse<CourseReviewVO>> {
  const response = await request<PageResponse<CourseReviewVO> | null>({
    url: `/courses/${String(courseId)}/reviews`,
    method: 'get',
    params,
  })

  return (
    response.data || {
      pageNum: Number(params.pageNum || 1),
      pageSize: Number(params.pageSize || 10),
      total: 0,
      totalPages: 0,
      records: [],
    }
  )
}

async function issueCertificate(courseId: string | number): Promise<CourseCertificateVO> {
  const response = await request<CourseCertificateVO>({
    url: `/courses/${String(courseId)}/certificate`,
    method: 'post',
  })
  return response.data
}

export {
  completeCourseVideoUpload,
  createArticle,
  createChapter,
  createCourse,
  createNote,
  createReview,
  createVideo,
  deleteArticle,
  deleteChapter,
  deleteVideo,
  getCourseDetail,
  getMyProgress,
  getVideoPlayInfo,
  initCourseVideoUpload,
  issueCertificate,
  updateArticle,
  updateChapter,
  updateVideo,
  listCourses,
  listNotes,
  listReviews,
  offlineCourse,
  publishCourse,
  updateCourse,
  updateProgress,
}
