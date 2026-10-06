import { request } from '@/utils/request'
import type {
  CreateInterviewAnswerRequest,
  CreateInterviewCategoryRequest,
  CreateInterviewQuestionRequest,
  CreateMockInterviewRequest,
  InterviewAnswerVO,
  InterviewCategoryPageParams,
  InterviewCategoryVO,
  InterviewCompanyVO,
  InterviewQuestionListParams,
  InterviewQuestionVO,
  MockInterviewVO,
  PageResponse,
  UpdateInterviewCategoryRequest,
  UpdateInterviewQuestionRequest,
  UpdateQuestionProgressRequest,
  UserQuestionProgressVO,
  UserWrongQuestionVO,
} from '@/api/types'

export const createInterviewQuestionApi = (data: CreateInterviewQuestionRequest) =>
  request<InterviewQuestionVO>({ url: '/interview/questions', method: 'post', data })

export const updateInterviewQuestionApi = (
  questionId: string,
  data: UpdateInterviewQuestionRequest,
) => request<InterviewQuestionVO>({ url: `/interview/questions/${questionId}`, method: 'put', data })

export const getInterviewQuestionDetailApi = (questionId: string) =>
  request<InterviewQuestionVO>({ url: `/interview/questions/${questionId}`, method: 'get' })

export const getInterviewQuestionListApi = (params: InterviewQuestionListParams) =>
  request<PageResponse<InterviewQuestionVO>>({ url: '/interview/questions', method: 'get', params })

export const createInterviewAnswerApi = (questionId: string, data: CreateInterviewAnswerRequest) =>
  request<InterviewAnswerVO>({
    url: `/interview/questions/${questionId}/answers`,
    method: 'post',
    data,
  })

export const getInterviewAnswerListApi = (questionId: string) =>
  request<InterviewAnswerVO[]>({ url: `/interview/questions/${questionId}/answers`, method: 'get' })

export const createInterviewCategoryApi = (data: CreateInterviewCategoryRequest) =>
  request<InterviewCategoryVO>({ url: '/interview/categories', method: 'post', data })

export const updateInterviewCategoryApi = (
  categoryId: string,
  data: UpdateInterviewCategoryRequest,
) => request<InterviewCategoryVO>({ url: `/interview/categories/${categoryId}`, method: 'put', data })

export const deleteInterviewCategoryApi = (categoryId: string) =>
  request<void>({ url: `/interview/categories/${categoryId}`, method: 'delete' })

export const getInterviewCategoryPageApi = (params: InterviewCategoryPageParams) =>
  request<PageResponse<InterviewCategoryVO>>({ url: '/interview/categories/page', method: 'get', params })

export const getInterviewCategoryListApi = () =>
  request<InterviewCategoryVO[]>({ url: '/interview/categories', method: 'get' })

export const getInterviewCompanyListApi = () =>
  request<InterviewCompanyVO[]>({ url: '/interview/companies', method: 'get' })

export const updateInterviewProgressApi = (
  questionId: string,
  data: UpdateQuestionProgressRequest,
) => request<UserQuestionProgressVO>({ url: `/interview/questions/${questionId}/progress`, method: 'put', data })

export const getInterviewProgressApi = (questionId: string) =>
  request<UserQuestionProgressVO>({ url: `/interview/questions/${questionId}/progress`, method: 'get' })

export const collectInterviewQuestionApi = (questionId: string) =>
  request<void>({ url: `/interview/questions/${questionId}/collect`, method: 'post' })

export const uncollectInterviewQuestionApi = (questionId: string) =>
  request<void>({ url: `/interview/questions/${questionId}/uncollect`, method: 'post' })

export const getCollectedInterviewQuestionListApi = (pageNum = 1, pageSize = 10) =>
  request<PageResponse<InterviewQuestionVO>>({
    url: '/interview/questions/collected',
    method: 'get',
    params: { pageNum, pageSize },
  })

export const getWrongInterviewQuestionListApi = (pageNum = 1, pageSize = 10, isResolved?: number) =>
  request<PageResponse<UserWrongQuestionVO>>({
    url: '/interview/wrong-questions',
    method: 'get',
    params: { pageNum, pageSize, isResolved },
  })

export const resolveWrongInterviewQuestionApi = (questionId: string) =>
  request<void>({ url: `/interview/questions/${questionId}/wrong-questions/resolve`, method: 'post' })

export const createMockInterviewApi = (data: CreateMockInterviewRequest) =>
  request<MockInterviewVO>({ url: '/interview/mock-interviews', method: 'post', data })

export const getMockInterviewListApi = (pageNum = 1, pageSize = 10) =>
  request<PageResponse<MockInterviewVO>>({
    url: '/interview/mock-interviews',
    method: 'get',
    params: { pageNum, pageSize },
  })
