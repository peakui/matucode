import type { AxiosResponse } from 'axios'
import { request } from './request'
import type {
  ApiResponse,
  CreateAnswerRequest,
  CreateQaCommentRequest,
  CreateQuestionRequest,
  ListQuestionsParams,
  PageParams,
  PageResponse,
  QaAnswerVO,
  QaCategoryVO,
  QaCommentVO,
  QaQuestionDetailVO,
  QaQuestionListItemVO,
  QaWordCloudVO,
  UpdateAnswerRequest,
  UpdateQuestionRequest,
} from './type/qaTypings'

const emptyPage = <T>(params: PageParams = {}): PageResponse<T> => ({
  pageNum: Number(params.pageNum || 1),
  pageSize: Number(params.pageSize || 10),
  total: 0,
  totalPages: 0,
  records: [],
})

async function createQuestion(data: CreateQuestionRequest): Promise<QaQuestionDetailVO> {
  const response: AxiosResponse<ApiResponse<QaQuestionDetailVO>> = await request.post('/qa/questions', data)
  return response.data.data
}

async function updateQuestion(questionId: string | number, data: UpdateQuestionRequest): Promise<QaQuestionDetailVO> {
  const response: AxiosResponse<ApiResponse<QaQuestionDetailVO>> = await request.put(`/qa/questions/${String(questionId)}`, data)
  return response.data.data
}

async function deleteQuestion(questionId: string | number): Promise<void> {
  await request.delete(`/qa/questions/${String(questionId)}`)
}

async function getQuestionDetail(questionId: string | number): Promise<QaQuestionDetailVO> {
  const response: AxiosResponse<ApiResponse<QaQuestionDetailVO>> = await request.get(`/qa/questions/${String(questionId)}`)
  return response.data.data
}

async function listQuestions(params: ListQuestionsParams = {}): Promise<PageResponse<QaQuestionListItemVO>> {
  const response: AxiosResponse<ApiResponse<PageResponse<QaQuestionListItemVO> | null>> = await request.get('/qa/questions', { params })
  return response.data.data || emptyPage<QaQuestionListItemVO>(params)
}

async function listMyQuestions(params: PageParams = {}): Promise<PageResponse<QaQuestionListItemVO>> {
  const response: AxiosResponse<ApiResponse<PageResponse<QaQuestionListItemVO> | null>> = await request.get('/qa/questions/mine', { params })
  return response.data.data || emptyPage<QaQuestionListItemVO>(params)
}

async function listMyFollowedQuestions(params: PageParams = {}): Promise<PageResponse<QaQuestionListItemVO>> {
  const response: AxiosResponse<ApiResponse<PageResponse<QaQuestionListItemVO> | null>> = await request.get('/qa/questions/following', { params })
  return response.data.data || emptyPage<QaQuestionListItemVO>(params)
}

async function listLikedQuestions(params: PageParams = {}): Promise<PageResponse<QaQuestionListItemVO>> {
  const response: AxiosResponse<ApiResponse<PageResponse<QaQuestionListItemVO> | null>> = await request.get('/qa/questions/liked', { params })
  return response.data.data || emptyPage<QaQuestionListItemVO>(params)
}

async function createAnswer(questionId: string | number, data: CreateAnswerRequest): Promise<QaAnswerVO> {
  const response: AxiosResponse<ApiResponse<QaAnswerVO>> = await request.post(`/qa/questions/${String(questionId)}/answers`, data)
  return response.data.data
}

async function listQuestionAnswers(questionId: string | number, params: PageParams = {}): Promise<PageResponse<QaAnswerVO>> {
  const response: AxiosResponse<ApiResponse<PageResponse<QaAnswerVO> | null>> = await request.get(`/qa/questions/${String(questionId)}/answers`, { params })
  return response.data.data || emptyPage<QaAnswerVO>(params)
}

async function updateAnswer(answerId: string | number, data: UpdateAnswerRequest): Promise<QaAnswerVO> {
  const response: AxiosResponse<ApiResponse<QaAnswerVO>> = await request.put(`/qa/answers/${String(answerId)}`, data)
  return response.data.data
}

async function listMyAnswers(params: PageParams = {}): Promise<PageResponse<QaAnswerVO>> {
  const response: AxiosResponse<ApiResponse<PageResponse<QaAnswerVO> | null>> = await request.get('/qa/answers/mine', { params })
  return response.data.data || emptyPage<QaAnswerVO>(params)
}

async function deleteAnswer(answerId: string | number): Promise<void> {
  await request.delete(`/qa/answers/${String(answerId)}`)
}

async function acceptAnswer(questionId: string | number, answerId: string | number): Promise<void> {
  await request.post(`/qa/questions/${String(questionId)}/answers/${String(answerId)}/accept`)
}

async function followQuestion(questionId: string | number): Promise<void> {
  await request.post(`/qa/questions/${String(questionId)}/follow`)
}

async function unfollowQuestion(questionId: string | number): Promise<void> {
  await request.post(`/qa/questions/${String(questionId)}/unfollow`)
}

async function shareQuestion(questionId: string | number): Promise<void> {
  await request.post(`/qa/questions/${String(questionId)}/share`)
}

async function voteQuestion(questionId: string | number, voteType: number): Promise<void> {
  await request.post(`/qa/questions/${String(questionId)}/vote`, null, { params: { voteType } })
}

async function unvoteQuestion(questionId: string | number): Promise<void> {
  await request.delete(`/qa/questions/${String(questionId)}/vote`)
}

async function voteAnswer(answerId: string | number, voteType: number): Promise<void> {
  await request.post(`/qa/answers/${String(answerId)}/vote`, null, { params: { voteType } })
}

async function createQaComment(data: CreateQaCommentRequest): Promise<QaCommentVO> {
  const response: AxiosResponse<ApiResponse<QaCommentVO>> = await request.post('/qa/comments', data)
  return response.data.data
}

async function deleteQaComment(commentId: string | number): Promise<void> {
  await request.delete(`/qa/comments/${String(commentId)}`)
}

async function likeQaComment(commentId: string | number): Promise<void> {
  await request.post(`/qa/comments/${String(commentId)}/like`)
}

async function unlikeQaComment(commentId: string | number): Promise<void> {
  await request.delete(`/qa/comments/${String(commentId)}/like`)
}

async function listQaComments(targetType: number, targetId: string | number): Promise<QaCommentVO[]> {
  const response: AxiosResponse<ApiResponse<QaCommentVO[] | null>> = await request.get('/qa/comments', { params: { targetType, targetId } })
  return response.data.data || []
}

async function listQaCategoryTree(): Promise<QaCategoryVO[]> {
  const response: AxiosResponse<ApiResponse<QaCategoryVO[] | null>> = await request.get('/qa/categories/tree')
  return response.data.data || []
}

async function listQaCategories(): Promise<QaCategoryVO[]> {
  const response: AxiosResponse<ApiResponse<QaCategoryVO[] | null>> = await request.get('/qa/categories')
  return response.data.data || []
}

async function getQuestionWordCloud(questionId: string | number): Promise<QaWordCloudVO> {
  const response: AxiosResponse<ApiResponse<QaWordCloudVO>> = await request.get(`/qa/questions/${String(questionId)}/word-cloud`)
  return response.data.data
}

export {
  acceptAnswer,
  createAnswer,
  createQaComment,
  createQuestion,
  deleteAnswer,
  deleteQaComment,
  deleteQuestion,
  followQuestion,
  getQuestionDetail,
  getQuestionWordCloud,
  likeQaComment,
  listMyAnswers,
  listMyFollowedQuestions,
  listLikedQuestions,
  listMyQuestions,
  listQaCategories,
  listQaCategoryTree,
  listQaComments,
  listQuestionAnswers,
  listQuestions,
  shareQuestion,
  unfollowQuestion,
  unlikeQaComment,
  updateAnswer,
  updateQuestion,
  unvoteQuestion,
  voteAnswer,
  voteQuestion,
}
