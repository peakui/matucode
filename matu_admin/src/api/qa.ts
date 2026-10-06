import { request } from '@/utils/request'
import type {
  CreateAnswerRequest,
  CreateQaCommentRequest,
  CreateQuestionRequest,
  PageResponse,
  QaAnswerVO,
  QaCategoryVO,
  QaCommentVO,
  QaQuestionDetailVO,
  QaQuestionListItemVO,
  QaQuestionListParams,
  QaWordCloudVO,
  UpdateAnswerRequest,
  UpdateQuestionRequest,
} from '@/api/types'

export const getQaQuestionListApi = (params: QaQuestionListParams) => {
  return request<PageResponse<QaQuestionListItemVO>>({
    url: '/qa/questions',
    method: 'get',
    params,
  })
}

export const getQaQuestionDetailApi = (questionId: string) => {
  return request<QaQuestionDetailVO>({
    url: `/qa/questions/${questionId}`,
    method: 'get',
  })
}

export const createQaQuestionApi = (data: CreateQuestionRequest) => {
  return request<QaQuestionDetailVO>({
    url: '/qa/questions',
    method: 'post',
    data,
  })
}

export const updateQaQuestionApi = (questionId: string, data: UpdateQuestionRequest) => {
  return request<QaQuestionDetailVO>({
    url: `/qa/questions/${questionId}`,
    method: 'put',
    data,
  })
}

export const deleteQaQuestionApi = (questionId: string) => {
  return request<null>({
    url: `/qa/questions/${questionId}`,
    method: 'delete',
  })
}

export const getQaMyQuestionListApi = (params: { pageNum?: number; pageSize?: number }) => {
  return request<PageResponse<QaQuestionListItemVO>>({
    url: '/qa/questions/mine',
    method: 'get',
    params,
  })
}

export const getQaFollowedQuestionListApi = (params: { pageNum?: number; pageSize?: number }) => {
  return request<PageResponse<QaQuestionListItemVO>>({
    url: '/qa/questions/following',
    method: 'get',
    params,
  })
}

export const createQaAnswerApi = (questionId: string, data: CreateAnswerRequest) => {
  return request<QaAnswerVO>({
    url: `/qa/questions/${questionId}/answers`,
    method: 'post',
    data,
  })
}

export const getQaAnswerListApi = (
  questionId: string,
  params: { pageNum?: number; pageSize?: number },
) => {
  return request<PageResponse<QaAnswerVO>>({
    url: `/qa/questions/${questionId}/answers`,
    method: 'get',
    params,
  })
}

export const updateQaAnswerApi = (answerId: string, data: UpdateAnswerRequest) => {
  return request<QaAnswerVO>({
    url: `/qa/answers/${answerId}`,
    method: 'put',
    data,
  })
}

export const getQaMyAnswerListApi = (params: { pageNum?: number; pageSize?: number }) => {
  return request<PageResponse<QaAnswerVO>>({
    url: '/qa/answers/mine',
    method: 'get',
    params,
  })
}

export const deleteQaAnswerApi = (answerId: string) => {
  return request<null>({
    url: `/qa/answers/${answerId}`,
    method: 'delete',
  })
}

export const acceptQaAnswerApi = (questionId: string, answerId: string) => {
  return request<null>({
    url: `/qa/questions/${questionId}/answers/${answerId}/accept`,
    method: 'post',
  })
}

export const followQaQuestionApi = (questionId: string) => {
  return request<null>({
    url: `/qa/questions/${questionId}/follow`,
    method: 'post',
  })
}

export const unfollowQaQuestionApi = (questionId: string) => {
  return request<null>({
    url: `/qa/questions/${questionId}/unfollow`,
    method: 'post',
  })
}

export const voteQaQuestionApi = (questionId: string, voteType: number) => {
  return request<null>({
    url: `/qa/questions/${questionId}/vote`,
    method: 'post',
    params: { voteType },
  })
}

export const voteQaAnswerApi = (answerId: string, voteType: number) => {
  return request<null>({
    url: `/qa/answers/${answerId}/vote`,
    method: 'post',
    params: { voteType },
  })
}

export const createQaCommentApi = (data: CreateQaCommentRequest) => {
  return request<QaCommentVO>({
    url: '/qa/comments',
    method: 'post',
    data,
  })
}

export const deleteQaCommentApi = (commentId: string) => {
  return request<null>({
    url: `/qa/comments/${commentId}`,
    method: 'delete',
  })
}

export const getQaCommentListApi = (params: { targetType: number; targetId: string }) => {
  return request<QaCommentVO[]>({
    url: '/qa/comments',
    method: 'get',
    params,
  })
}

export const getQaCategoryTreeApi = () => {
  return request<QaCategoryVO[]>({
    url: '/qa/categories/tree',
    method: 'get',
  })
}

export const getQaCategoryListApi = () => {
  return request<QaCategoryVO[]>({
    url: '/qa/categories',
    method: 'get',
  })
}

export const getQaWordCloudApi = (questionId: string) => {
  return request<QaWordCloudVO>({
    url: `/qa/questions/${questionId}/word-cloud`,
    method: 'get',
  })
}
