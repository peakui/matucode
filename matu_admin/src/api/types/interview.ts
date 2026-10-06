export interface InterviewCategoryPageParams {
  parentId?: string
  status?: number
  keyword?: string
  pageNum?: number
  pageSize?: number
}

export interface InterviewQuestionListParams {
  categoryId?: string
  companyId?: string
  difficulty?: number
  isLocked?: number
  keyword?: string
  pageNum?: number
  pageSize?: number
}

export interface InterviewQuestionVO {
  id: string
  questionNo?: string
  title: string
  content?: string
  answer?: string
  categoryId?: string
  categoryName?: string
  companyId?: string
  companyName?: string
  publisherId?: string
  positionTags?: string[]
  difficulty: number
  frequency?: number
  viewCount?: number
  collectCount?: number
  isLocked: number
  unlockDays?: number
  status?: number
  createdAt?: string
  updatedAt?: string
}

export interface InterviewAnswerVO {
  id: string
  questionId: string
  content: string
  sortOrder?: number
  isOfficial?: number
  createdAt?: string
  updatedAt?: string
}

export interface InterviewCategoryVO {
  id: string
  parentId?: string
  categoryName: string
  categoryDesc?: string
  iconUrl?: string
  questionCount?: number
  sortOrder?: number
  status?: number
  createdAt?: string
  children?: InterviewCategoryVO[]
}

export interface CreateInterviewCategoryRequest {
  parentId?: string
  categoryName: string
  categoryDesc?: string
  iconUrl?: string
  sortOrder?: number
  status?: number
}

export interface UpdateInterviewCategoryRequest extends Partial<CreateInterviewCategoryRequest> {}

export interface InterviewCompanyVO {
  id: string
  companyName: string
  companyDesc?: string
  sortOrder?: number
  status?: number
}

export interface UserQuestionProgressVO {
  id?: string
  questionId: string
  progressStatus?: number
  notes?: string
  lastAnsweredAt?: string
  updatedAt?: string
}

export interface UserWrongQuestionVO {
  id: string
  questionId: string
  isResolved: number
  wrongCount?: number
  lastWrongAt?: string
  question?: InterviewQuestionVO
}

export interface MockInterviewVO {
  id: string
  title?: string
  categoryId?: string
  companyId?: string
  questionCount?: number
  status?: number
  createdAt?: string
}

export interface CreateInterviewQuestionRequest {
  questionNo: string
  title: string
  content: string
  answer?: string
  categoryId?: string
  companyId?: string
  positionTags?: string[]
  difficulty: number
  isLocked?: number
  unlockDays?: number
  status?: number
}

export interface UpdateInterviewQuestionRequest extends Partial<Omit<CreateInterviewQuestionRequest, 'questionNo'>> {
  frequency?: number
}

export interface CreateInterviewAnswerRequest {
  content: string
  sortOrder?: number
  isOfficial?: number
}

export interface UpdateQuestionProgressRequest {
  progressStatus: number
  notes?: string
}

export interface CreateMockInterviewRequest {
  title?: string
  categoryId?: string
  companyId?: string
  questionIds?: string[]
}
