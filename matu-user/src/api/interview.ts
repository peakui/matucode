import type { AxiosResponse } from 'axios'
import { request } from './request'
import type {
  CreateInterviewAnswerRequest,
  CreateInterviewQuestionRequest,
  CreateMockInterviewRequest,
  InterviewAnswerVO,
  InterviewApiResponse,
  InterviewCategoryVO,
  InterviewCompanyVO,
  InterviewPageResponse,
  InterviewQuestionVO,
  ListInterviewQuestionsParams,
  ListWrongQuestionsParams,
  MockInterviewVO,
  UpdateInterviewQuestionRequest,
  UpdateQuestionProgressRequest,
  UserQuestionProgressVO,
  UserWrongQuestionVO,
} from './type/interviewTypings'

const INTERVIEW_PREFIX = '/interview'

const normalizePageResponse = <T>(data: InterviewPageResponse<T> | null | undefined, pageNum: number, pageSize: number): InterviewPageResponse<T> => {
  const pageData = (data || {}) as InterviewPageResponse<T> & {
    list?: T[]
    rows?: T[]
    items?: T[]
  }

  return {
    pageNum: Number(pageData.pageNum || pageNum),
    pageSize: Number(pageData.pageSize || pageSize),
    total: Number(pageData.total || 0),
    totalPages: Number(pageData.totalPages || 0),
    records: pageData.records || pageData.list || pageData.rows || pageData.items || [],
  }
}

async function createInterviewQuestion(data: CreateInterviewQuestionRequest): Promise<InterviewQuestionVO> {
  const response: AxiosResponse<InterviewApiResponse<InterviewQuestionVO>> = await request.post(`${INTERVIEW_PREFIX}/questions`, data)
  return response.data.data
}

async function updateInterviewQuestion(questionId: number, data: UpdateInterviewQuestionRequest): Promise<InterviewQuestionVO> {
  const response: AxiosResponse<InterviewApiResponse<InterviewQuestionVO>> = await request.put(`${INTERVIEW_PREFIX}/questions/${questionId}`, data)
  return response.data.data
}

async function getInterviewQuestionDetail(questionId: number | string): Promise<InterviewQuestionVO> {
  const response: AxiosResponse<InterviewApiResponse<InterviewQuestionVO>> = await request.get(`${INTERVIEW_PREFIX}/questions/${questionId}`)
  return response.data.data
}

async function listInterviewQuestions(params: ListInterviewQuestionsParams = {}): Promise<InterviewPageResponse<InterviewQuestionVO>> {
  const response: AxiosResponse<InterviewApiResponse<InterviewPageResponse<InterviewQuestionVO> | null>> = await request.get(`${INTERVIEW_PREFIX}/questions`, { params })
  return normalizePageResponse(response.data.data, Number(params.pageNum || 1), Number(params.pageSize || 10))
}

async function createInterviewAnswer(questionId: number, data: CreateInterviewAnswerRequest): Promise<InterviewAnswerVO> {
  const response: AxiosResponse<InterviewApiResponse<InterviewAnswerVO>> = await request.post(`${INTERVIEW_PREFIX}/questions/${questionId}/answers`, data)
  return response.data.data
}

async function listInterviewAnswers(questionId: number): Promise<InterviewAnswerVO[]> {
  const response: AxiosResponse<InterviewApiResponse<InterviewAnswerVO[] | null>> = await request.get(`${INTERVIEW_PREFIX}/questions/${questionId}/answers`)
  return response.data.data || []
}

async function listInterviewCategories(): Promise<InterviewCategoryVO[]> {
  const response: AxiosResponse<InterviewApiResponse<InterviewCategoryVO[] | null>> = await request.get(`${INTERVIEW_PREFIX}/categories`)
  return response.data.data || []
}

async function listInterviewCompanies(): Promise<InterviewCompanyVO[]> {
  const response: AxiosResponse<InterviewApiResponse<InterviewCompanyVO[] | null>> = await request.get(`${INTERVIEW_PREFIX}/companies`)
  return response.data.data || []
}

async function updateInterviewQuestionProgress(questionId: number, data: UpdateQuestionProgressRequest): Promise<UserQuestionProgressVO> {
  const response: AxiosResponse<InterviewApiResponse<UserQuestionProgressVO>> = await request.put(`${INTERVIEW_PREFIX}/questions/${questionId}/progress`, data)
  return response.data.data
}

async function getInterviewQuestionProgress(questionId: number | string): Promise<UserQuestionProgressVO> {
  const response: AxiosResponse<InterviewApiResponse<UserQuestionProgressVO>> = await request.get(`${INTERVIEW_PREFIX}/questions/${questionId}/progress`)
  return response.data.data
}

async function collectInterviewQuestion(questionId: number | string): Promise<void> {
  await request.post(`${INTERVIEW_PREFIX}/questions/${questionId}/collect`)
}

async function uncollectInterviewQuestion(questionId: number): Promise<void> {
  await request.post(`${INTERVIEW_PREFIX}/questions/${questionId}/uncollect`)
}

async function listCollectedInterviewQuestions(pageNum = 1, pageSize = 10): Promise<InterviewPageResponse<InterviewQuestionVO>> {
  const response: AxiosResponse<InterviewApiResponse<InterviewPageResponse<InterviewQuestionVO> | null>> = await request.get(`${INTERVIEW_PREFIX}/questions/collected`, {
    params: { pageNum, pageSize },
  })
  return normalizePageResponse(response.data.data, pageNum, pageSize)
}

async function listWrongInterviewQuestions(params: ListWrongQuestionsParams = {}): Promise<InterviewPageResponse<UserWrongQuestionVO>> {
  const response: AxiosResponse<InterviewApiResponse<InterviewPageResponse<UserWrongQuestionVO> | null>> = await request.get(`${INTERVIEW_PREFIX}/wrong-questions`, {
    params,
  })
  return normalizePageResponse(response.data.data, Number(params.pageNum || 1), Number(params.pageSize || 10))
}

async function resolveWrongInterviewQuestion(questionId: number): Promise<void> {
  await request.post(`${INTERVIEW_PREFIX}/questions/${questionId}/wrong-questions/resolve`)
}

async function createMockInterview(data: CreateMockInterviewRequest): Promise<MockInterviewVO> {
  const response: AxiosResponse<InterviewApiResponse<MockInterviewVO>> = await request.post(`${INTERVIEW_PREFIX}/mock-interviews`, data)
  return response.data.data
}

async function listMockInterviews(pageNum = 1, pageSize = 10): Promise<InterviewPageResponse<MockInterviewVO>> {
  const response: AxiosResponse<InterviewApiResponse<InterviewPageResponse<MockInterviewVO> | null>> = await request.get(`${INTERVIEW_PREFIX}/mock-interviews`, {
    params: { pageNum, pageSize },
  })
  return normalizePageResponse(response.data.data, pageNum, pageSize)
}

export {
  collectInterviewQuestion,
  createInterviewAnswer,
  createInterviewQuestion,
  createMockInterview,
  getInterviewQuestionDetail,
  getInterviewQuestionProgress,
  listCollectedInterviewQuestions,
  listInterviewAnswers,
  listInterviewCategories,
  listInterviewCompanies,
  listInterviewQuestions,
  listMockInterviews,
  listWrongInterviewQuestions,
  normalizePageResponse,
  resolveWrongInterviewQuestion,
  uncollectInterviewQuestion,
  updateInterviewQuestion,
  updateInterviewQuestionProgress,
}
