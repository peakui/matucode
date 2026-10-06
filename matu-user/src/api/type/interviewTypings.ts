import type { ApiResponse, PageResponse } from './postTypings'

export interface ListInterviewQuestionsParams {
  categoryId?: number
  companyId?: number
  difficulty?: number
  isLocked?: number
  keyword?: string
  pageNum?: number
  pageSize?: number
}

export interface ListWrongQuestionsParams {
  pageNum?: number
  pageSize?: number
  isResolved?: number
}

export interface CreateInterviewQuestionRequest {
  questionNo: string
  title: string
  content: string
  answer?: string
  categoryId?: number
  companyId?: number
  positionTags?: string[]
  difficulty: number
  isLocked?: number
  unlockDays?: number
  status?: number
}

export interface UpdateInterviewQuestionRequest {
  title: string
  content: string
  answer?: string
  categoryId?: number
  companyId?: number
  positionTags?: string[]
  difficulty?: number
  frequency?: number
  isLocked?: number
  unlockDays?: number
  status?: number
}

export interface CreateInterviewAnswerRequest {
  content: string
  answerType?: number
  contentType?: number
  codeSnippet?: string
  isOfficial?: number
  isLocked?: number
  unlockDays?: number
  status?: number
}

export interface UpdateQuestionProgressRequest {
  status: number
  answerContent?: string
  masteryLevel?: number
  practiceCount?: number
  wrongReason?: string
  addToWrongBook?: number
}

export interface CreateMockInterviewRequest {
  interviewTitle: string
  position?: string
  company?: string
  questionIds?: number[]
  durationMinutes?: number
}

export interface InterviewAnswerVO {
  id?: number
  questionId?: number
  answerType?: number
  userId?: number
  content?: string
  contentType?: number
  codeSnippet?: string
  likeCount?: number
  isOfficial?: number
  isLocked?: number
  unlockDays?: number
  status?: number
  createdAt?: string
  updatedAt?: string
}

export interface InterviewCategoryVO {
  id?: number
  parentId?: number
  categoryName?: string
  categoryDesc?: string
  iconUrl?: string
  questionCount?: number
  sortOrder?: number
  status?: number
  createdAt?: string
  children?: InterviewCategoryVO[]
}

export interface InterviewCompanyVO {
  id?: number
  companyName?: string
  companyLogo?: string
  companyType?: string
  questionCount?: number
  status?: number
  createdAt?: string
}

export interface InterviewQuestionVO {
  id?: number
  questionNo?: string
  title?: string
  content?: string
  answer?: string
  answerVisible?: boolean
  categoryId?: number
  categoryName?: string
  companyId?: number
  companyName?: string
  publisherId?: number
  positionTags?: string[]
  difficulty?: number
  frequency?: number
  viewCount?: number
  collectCount?: number
  isLocked?: number
  unlockDays?: number
  status?: number
  createdAt?: string
  updatedAt?: string
}

export interface MockInterviewVO {
  id?: number
  userId?: number
  interviewTitle?: string
  position?: string
  company?: string
  questionIds?: number[]
  totalScore?: number
  userScore?: number
  durationMinutes?: number
  actualDuration?: number
  status?: number
  reportJson?: string
  startedAt?: string
  completedAt?: string
  createdAt?: string
}

export interface UserQuestionProgressVO {
  id?: number
  userId?: number
  questionId?: number
  questionType?: number
  status?: number
  answerContent?: string
  lastPracticeTime?: string
  practiceCount?: number
  masteryLevel?: number
  nextReviewTime?: string
  createdAt?: string
  updatedAt?: string
}

export interface UserWrongQuestionVO {
  id?: number
  userId?: number
  questionId?: number
  questionType?: number
  wrongCount?: number
  lastWrongTime?: string
  wrongReason?: string
  isResolved?: number
  resolvedAt?: string
  createdAt?: string
  updatedAt?: string
}

export type InterviewApiResponse<T> = ApiResponse<T>
export type InterviewPageResponse<T> = PageResponse<T>
