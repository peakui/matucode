import type { AxiosResponse } from 'axios'
import { request } from './request'
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
  PageResponse,
  UpdateArticleRequest,
  UpdateCourseRequest,
  UpdateProgressRequest,
  UpdateVideoRequest,
  VideoPlayVO,
} from './type/courseTypings'

const isSuccessResponse = (response: ApiResponse<unknown>) => (
  response.code === 0 || response.code === 200 || response.code === 20000 || response.success === true
)

const getErrorMessage = (response: ApiResponse<unknown>, fallback: string) => response.message || response.msg || fallback

async function listCourses(params: CourseListParams = {}): Promise<PageResponse<CourseListItemVO>> {
  const response: AxiosResponse<ApiResponse<PageResponse<CourseListItemVO> | null>> = await request.get('/courses', { params })
  if (!isSuccessResponse(response.data)) {
    throw new Error(getErrorMessage(response.data, '课程列表加载失败'))
  }
  return response.data.data || { pageNum: 1, pageSize: 10, total: 0, totalPages: 0, records: [] }
}

async function getCourseDetail(courseId: string | number): Promise<CourseDetailVO> {
  const response: AxiosResponse<ApiResponse<CourseDetailVO>> = await request.get(`/courses/${String(courseId)}`)
  return response.data.data
}

async function getVideoPlayInfo(videoId: string | number): Promise<VideoPlayVO> {
  const response: AxiosResponse<ApiResponse<VideoPlayVO>> = await request.get(`/courses/videos/${String(videoId)}/play`)
  return response.data.data
}

async function createCourse(data: CreateCourseRequest): Promise<CourseDetailVO> {
  const response: AxiosResponse<ApiResponse<CourseDetailVO>> = await request.post('/courses', data)
  return response.data.data
}

async function updateCourse(courseId: string | number, data: UpdateCourseRequest): Promise<CourseDetailVO> {
  const response: AxiosResponse<ApiResponse<CourseDetailVO>> = await request.put(`/courses/${String(courseId)}`, data)
  return response.data.data
}

async function publishCourse(courseId: string | number): Promise<void> {
  await request.post(`/courses/${String(courseId)}/publish`)
}

async function offlineCourse(courseId: string | number): Promise<void> {
  await request.post(`/courses/${String(courseId)}/offline`)
}

async function createChapter(data: CreateChapterRequest): Promise<CourseChapterVO> {
  const response: AxiosResponse<ApiResponse<CourseChapterVO>> = await request.post('/courses/chapters', data)
  return response.data.data
}

async function initCourseVideoUpload(data: InitCourseVideoUploadRequest): Promise<CourseVideoUploadInitVO> {
  const response: AxiosResponse<ApiResponse<CourseVideoUploadInitVO>> = await request.post('/courses/videos/upload/init', data)
  return response.data.data
}

async function completeCourseVideoUpload(data: CompleteCourseVideoUploadRequest): Promise<CourseVideoVO> {
  const response: AxiosResponse<ApiResponse<CourseVideoVO>> = await request.post('/courses/videos/upload/complete', data)
  return response.data.data
}

async function createVideo(data: CreateVideoRequest): Promise<CourseVideoVO> {
  const response: AxiosResponse<ApiResponse<CourseVideoVO>> = await request.post('/courses/videos', data)
  return response.data.data
}

async function createArticle(data: CreateArticleRequest): Promise<CourseArticleVO> {
  const response: AxiosResponse<ApiResponse<CourseArticleVO>> = await request.post('/courses/articles', data)
  return response.data.data
}

async function updateVideo(videoId: string | number, data: UpdateVideoRequest): Promise<CourseVideoVO> {
  const response: AxiosResponse<ApiResponse<CourseVideoVO>> = await request.put(`/courses/videos/${String(videoId)}`, data)
  return response.data.data
}

async function deleteVideo(videoId: string | number): Promise<void> {
  await request.delete(`/courses/videos/${String(videoId)}`)
}

async function updateArticle(articleId: string | number, data: UpdateArticleRequest): Promise<CourseArticleVO> {
  const response: AxiosResponse<ApiResponse<CourseArticleVO>> = await request.put(`/courses/articles/${String(articleId)}`, data)
  return response.data.data
}

async function deleteArticle(articleId: string | number): Promise<void> {
  await request.delete(`/courses/articles/${String(articleId)}`)
}

async function updateProgress(courseId: string | number, data: UpdateProgressRequest): Promise<CourseProgressVO> {
  const response: AxiosResponse<ApiResponse<CourseProgressVO>> = await request.post(`/courses/${String(courseId)}/progress`, data)
  return response.data.data
}

async function getMyProgress(courseId: string | number): Promise<CourseProgressVO[]> {
  const response: AxiosResponse<ApiResponse<CourseProgressVO[]>> = await request.get(`/courses/${String(courseId)}/progress/mine`)
  return response.data.data || []
}

async function createNote(data: CreateNoteRequest): Promise<CourseNoteVO> {
  const response: AxiosResponse<ApiResponse<CourseNoteVO>> = await request.post('/courses/notes', data)
  return response.data.data
}

async function listNotes(params: { courseId: string | number; videoId?: string | number; onlyPublic?: boolean; pageNum?: number; pageSize?: number }): Promise<PageResponse<CourseNoteVO>> {
  const response: AxiosResponse<ApiResponse<PageResponse<CourseNoteVO> | null>> = await request.get('/courses/notes', { params })
  return response.data.data || { pageNum: Number(params.pageNum || 1), pageSize: Number(params.pageSize || 10), total: 0, totalPages: 0, records: [] }
}

async function createReview(data: CreateReviewRequest): Promise<CourseReviewVO> {
  const response: AxiosResponse<ApiResponse<CourseReviewVO>> = await request.post('/courses/reviews', data)
  return response.data.data
}

async function listReviews(courseId: string | number, params: { pageNum?: number; pageSize?: number } = {}): Promise<PageResponse<CourseReviewVO>> {
  const response: AxiosResponse<ApiResponse<PageResponse<CourseReviewVO> | null>> = await request.get(`/courses/${String(courseId)}/reviews`, { params })
  return response.data.data || { pageNum: Number(params.pageNum || 1), pageSize: Number(params.pageSize || 10), total: 0, totalPages: 0, records: [] }
}

async function issueCertificate(courseId: string | number): Promise<CourseCertificateVO> {
  const response: AxiosResponse<ApiResponse<CourseCertificateVO>> = await request.post(`/courses/${String(courseId)}/certificate`)
  return response.data.data
}

export {
  completeCourseVideoUpload,
  createArticle,
  createChapter,
  createCourse,
  deleteArticle,
  deleteVideo,
  createNote,
  createReview,
  createVideo,
  getCourseDetail,
  getMyProgress,
  getVideoPlayInfo,
  initCourseVideoUpload,
  issueCertificate,
  listCourses,
  listNotes,
  listReviews,
  offlineCourse,
  publishCourse,
  updateArticle,
  updateCourse,
  updateProgress,
  updateVideo,
}
