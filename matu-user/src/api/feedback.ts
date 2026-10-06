import type { AxiosResponse } from 'axios'
import { request } from './request'
import type { ApiResponse } from './type/postTypings'
import type {
  CreateFeedbackRequest,
  FeedbackDetailVO,
  FeedbackPageResponse,
  ListMyFeedbacksParams,
} from './type/feedbackTypings'

const getFallbackFeedbackPage = (params: ListMyFeedbacksParams): FeedbackPageResponse => ({
  pageNum: Number(params.pageNum || 1),
  pageSize: Number(params.pageSize || 10),
  total: 0,
  totalPages: 0,
  records: [],
})

async function createFeedback(data: CreateFeedbackRequest): Promise<FeedbackDetailVO> {
  const response: AxiosResponse<ApiResponse<FeedbackDetailVO>> = await request.post('/feedbacks', data)
  return response.data.data
}

async function listMyFeedbacks(params: ListMyFeedbacksParams = {}): Promise<FeedbackPageResponse> {
  const response: AxiosResponse<ApiResponse<FeedbackPageResponse | null>> = await request.get('/feedbacks/mine', { params })
  return response.data.data || getFallbackFeedbackPage(params)
}

async function getFeedbackDetail(id: string | number): Promise<FeedbackDetailVO> {
  const response: AxiosResponse<ApiResponse<FeedbackDetailVO>> = await request.get(`/feedbacks/${String(id)}`)
  return response.data.data
}

export { createFeedback, getFeedbackDetail, listMyFeedbacks }